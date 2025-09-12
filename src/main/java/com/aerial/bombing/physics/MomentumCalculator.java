package com.aerial.bombing.physics;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * 高精度动量计算器，用于计算投掷物的初始速度（模拟现实战斗机投弹）
 */
public class MomentumCalculator {

    /**
     * 计算高精度的真实投弹动量，模拟现代战斗机精确投弹效果
     * 考虑因素：
     * 1. 玩家完整的三维速度向量（惯性）
     * 2. 重力影响（初始垂直速度为0，依靠重力下落）
     * 3. 空气阻力（在游戏中忽略，但调整了初始速度）
     * 4. 投弹高度和时机的精确计算
     * 5. 玩家姿态对投弹轨迹的影响
     *
     * @param player 玩家实体
     * @return 炸弹的初始速度向量
     */
    public static Vec3d calculateRealisticMomentum(PlayerEntity player) {
        // 获取玩家当前速度向量
        Vec3d playerVelocity = player.getVelocity();

        // 获取玩家朝向向量
        Vec3d lookVector = player.getRotationVec(1.0F);

        // 计算玩家水平速度（XZ平面）
        double horizontalSpeed = Math.sqrt(playerVelocity.x * playerVelocity.x + playerVelocity.z * playerVelocity.z);

        // 计算玩家垂直速度（Y轴）
        double verticalSpeed = playerVelocity.y;

        // 根据玩家俯仰角度调整投弹参数
        double pitch = Math.asin(-lookVector.y); // 俯仰角（弧度）

        // 确定投弹的初始速度分量
        double initialX, initialY, initialZ;

        // X和Z分量：继承玩家水平速度，但根据姿态微调
        initialX = playerVelocity.x;
        initialZ = playerVelocity.z;

        // Y分量：初始垂直速度基于玩家姿态
        // 俯冲时给予向下的初速度，爬升时给予向上的初速度
        double pitchFactor = -Math.sin(pitch) * 0.3; // 根据俯仰角调整
        initialY = verticalSpeed + pitchFactor;

        // 应用轻微的空气动力学效应（在Minecraft中简化处理）
        // 水平速度会因为投弹而略微减少
        double dragEffect = 0.98;
        initialX *= dragEffect;
        initialZ *= dragEffect;

        return new Vec3d(initialX, initialY, initialZ);
    }

    /**
     * 计算高精度的投弹位置，确保投弹点与玩家姿态匹配
     * @param player 玩家实体
     * @return 投弹位置
     */
    public static Vec3d calculateDropPosition(PlayerEntity player) {
        // 获取玩家眼睛位置
        Vec3d eyePos = player.getEyePos();

        // 获取玩家朝向向量
        Vec3d lookVector = player.getRotationVec(1.0F);

        // 获取玩家当前速度向量
        Vec3d playerVelocity = player.getVelocity();

        // 计算投弹位置偏移量
        Vec3d offset;

        // 根据玩家运动状态确定投弹位置
        double horizontalSpeed = Math.sqrt(playerVelocity.x * playerVelocity.x + playerVelocity.z * playerVelocity.z);
        double verticalSpeed = Math.abs(playerVelocity.y);

        // 基础偏移：玩家前方1.5个单位，下方1.2个单位
        Vec3d forward = new Vec3d(lookVector.x, 0, lookVector.z).normalize();
        offset = forward.multiply(1.5).add(0, -1.2, 0);

        // 根据运动状态调整偏移
        if (horizontalSpeed > 0.5) {
            // 高速飞行时，投弹点稍微靠前
            offset = offset.add(forward.multiply(0.5));
        }

        if (verticalSpeed > 0.3) {
            // 垂直运动时，调整高度
            if (playerVelocity.y > 0) {
                // 爬升时，投弹点更低
                offset = offset.add(0, -0.3, 0);
            } else {
                // 俯冲时，投弹点稍高
                offset = offset.add(0, 0.2, 0);
            }
        }

        // 根据俯仰角进一步调整
        double pitch = Math.asin(-lookVector.y);
        if (Math.abs(pitch) > 0.3) { // 俯仰角大于17度时
            double pitchAdjust = Math.sin(pitch) * 0.5;
            offset = offset.add(0, -pitchAdjust, 0);
        }

        return eyePos.add(offset);
    }

    /**
     * 计算预测命中点（仅供显示参考）
     * @param player 玩家实体
     * @param gravity 重力加速度（Minecraft中约为0.08）
     * @param fuseTicks 引信时间（tick）
     * @return 预测的命中点
     */
    public static Vec3d predictImpactPoint(PlayerEntity player, double gravity, int fuseTicks) {
        // 获取初始投弹参数
        Vec3d initialPosition = calculateDropPosition(player);
        Vec3d initialVelocity = calculateRealisticMomentum(player);

        // 计算飞行时间（秒）
        double flightTime = fuseTicks / 20.0;

        // 计算各轴位移
        double x = initialPosition.x + initialVelocity.x * flightTime;
        double z = initialPosition.z + initialVelocity.z * flightTime;

        // 垂直位移计算（考虑重力）
        double y = initialPosition.y + initialVelocity.y * flightTime - 0.5 * gravity * flightTime * flightTime;

        return new Vec3d(x, y, z);
    }
}
