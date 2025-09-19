package com.aerial.bombing.util;

import com.aerial.bombing.entity.MissileData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Optional;

public class ExplosionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger("ExplosionUtils");

    public static void triggerExplosion(Entity entity) {
        if (entity == null || entity.getWorld().isClient || entity.isRemoved()) {
            return;
        }

        LOGGER.info("开始尝试引爆实体: {}", entity.getType().getUntranslatedName());

        // 优先级 1: MissileData 接口
        if (entity instanceof MissileData missile) {
            LOGGER.info("[引爆策略 1] 成功: 实体实现了 MissileData 接口，调用 detonate()。");
            missile.detonate();
            return;
        }

        // 优先级 2: 反射调用
        try {
            Optional<Method> method = findMethod(entity.getClass(), "explode", "detonate", "explodeServer", "performTNTExplosion");
            if (method.isPresent()) {
                LOGGER.info("[引爆策略 2] 尝试: 通过反射找到并调用 '{}' 方法。", method.get().getName());
                method.get().setAccessible(true);
                method.get().invoke(entity);
                if(entity.isRemoved()){
                    LOGGER.info("[引爆策略 2] 成功: 实体在反射调用后被移除。");
                    return;
                } else {
                    LOGGER.warn("[引爆策略 2] 警告: 反射调用后实体依然存在。");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[引爆策略 2] 异常: 反射调用失败: ", e);
        }

        // 优先级 3: 模拟爆炸伤害
        LOGGER.info("[引爆策略 3] 尝试: 使用模拟爆炸伤害。");
        DamageSource explosionDamage = entity.getDamageSources().explosion(null, null);
        entity.damage(explosionDamage, 200.0f);
        if (entity.isRemoved()) {
            LOGGER.info("[引爆策略 3] 成功: 实体在受到爆炸伤害后被移除。");
            return;
        }

        // 优先级 4 (新): 时间加速引爆
        LOGGER.warn("[引爆策略 4] 尝试: 实体对常规引爆方式无反应，尝试'时间加速'引爆 (连续调用 tick() 20次)。");
        for (int i = 0; i < 20; i++) {
            if (!entity.isRemoved()) {
                entity.tick();
            } else {
                break;
            }
        }

        if (entity.isRemoved()) {
            LOGGER.info("[引爆策略 4] 成功: 实体在'时间加速'后被移除/引爆。");
            return;
        }

        // 最终备用方案
        LOGGER.error("最终引爆失败！实体 {} 在所有引爆尝试后依然存在！将强制移除。", entity.getType().getUntranslatedName());
        entity.discard();
    }

    private static Optional<Method> findMethod(Class<?> clazz, String... methodNames) {
        for (String methodName : methodNames) {
            for (Method method : clazz.getMethods()) { // 使用 getMethods 也能找到父类的方法
                if (method.getName().equals(methodName)) {
                    method.setAccessible(true);
                    return Optional.of(method);
                }
            }
        }
        return Optional.empty();
    }
}
