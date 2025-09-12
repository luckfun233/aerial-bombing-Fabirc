package com.aerial.bombing.physics;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * 高级动量计算器，实现真实的物理模拟
 * 包括角速度继承、增强阻力模型等
 */
public class AdvancedMomentumCalculator {

    // 物理常数
    private static final double GRAVITY = 0.08; // Minecraft中的重力加速度
    private static final double AIR_DENSITY = 1.225; // 空气密度 (kg/m³)
    private static final double DRAG_COEFFICIENT = 0.47; // 阻力系数（球体）
    private static final double BOMB_MASS = 5.0; // 炸弹质量 (kg)
    private static final double BOMB_RADIUS = 0.25; // 炸弹半径 (m)

    /**
     * 计算高精度的真实投弹动量，包含角速度继承和高级阻力模型
     *
     * @param player 玩家实体
     * @param config 配置参数
     * @return 炸弹的初始运动状态
     */
    public static BombMotionState calculateAdvancedMomentum(PlayerEntity player, AdvancedPhysicsConfig config) {
        // 获取玩家当前线速度
        Vec3d playerLinearVelocity = player.getVelocity();

        // 获取玩家角速度（通过历史姿态数据估算）
        Vec3d playerAngularVelocity = estimateAngularVelocity(player);

        // 计算投弹点相对于玩家质心的位置向量
        Vec3d dropOffset = calculateDropOffset(player);
        Vec3d dropPosition = player.getPos().add(0, player.getEyeHeight(player.getPose()), 0).add(dropOffset);

        // 计算由于角速度产生的切向速度
        Vec3d tangentialVelocity = calculateTangentialVelocity(playerAngularVelocity, dropOffset);

        // 总的初始速度 = 玩家线速度 + 切向速度
        Vec3d initialVelocity = playerLinearVelocity.add(tangentialVelocity);

        // 应用配置的惯性系数
        initialVelocity = applyInertiaCoefficients(initialVelocity, config);

        // 添加手动投掷力量
        Vec3d throwForce = calculateThrowForce(player, config);
        initialVelocity = initialVelocity.add(throwForce);

        // 创建运动状态对象
        BombMotionState motionState = new BombMotionState();
        motionState.position = dropPosition;
        motionState.velocity = initialVelocity;
        motionState.angularVelocity = playerAngularVelocity.multiply(0.3); // 炸弹继承部分角速度

        return motionState;
    }

    /**
     * 估算玩家的角速度（通过姿态变化）
     * 注意：这在Minecraft中是近似计算，因为没有直接的角速度API
     *
     * @param player 玩家实体
     * @return 估算的角速度向量
     */
    private static Vec3d estimateAngularVelocity(PlayerEntity player) {
        // 在实际实现中，我们需要跟踪玩家的历史姿态来计算角速度
        // 这里提供一个简化的估算方法

        // 获取玩家当前朝向
        Vec3d currentLook = player.getRotationVec(1.0F);

        // 简化估算：假设角速度与朝向变化率成正比
        // 在实际应用中，应该通过历史数据计算
        double yawRate = 0; // 偏航角速度
        double pitchRate = 0; // 俯仰角速度
        double rollRate = 0; // 滚转角速度

        // 滚转角速度估算（根据侧向移动）
        Vec3d moveDirection = new Vec3d(player.sidewaysSpeed, 0, player.forwardSpeed);
        if (moveDirection.lengthSquared() > 0.01) {
            rollRate = moveDirection.length() * 0.5;
        }

        // 返回角速度向量（单位：弧度/秒）
        return new Vec3d(pitchRate, yawRate, rollRate);
    }

    /**
     * 计算由于角速度产生的切向速度
     * 使用公式: v = ω × r (叉乘)
     *
     * @param angularVelocity 角速度向量
     * @param positionVector 位置向量（相对于旋转中心）
     * @return 切向速度向量
     */
    private static Vec3d calculateTangentialVelocity(Vec3d angularVelocity, Vec3d positionVector) {
        // 计算叉乘: ω × r
        double vx = angularVelocity.y * positionVector.z - angularVelocity.z * positionVector.y;
        double vy = angularVelocity.z * positionVector.x - angularVelocity.x * positionVector.z;
        double vz = angularVelocity.x * positionVector.y - angularVelocity.y * positionVector.x;

        return new Vec3d(vx, vy, vz);
    }

    /**
     * 计算投弹点相对于玩家质心的偏移
     *
     * @param player 玩家实体
     * @return 偏移向量
     */
    private static Vec3d calculateDropOffset(PlayerEntity player) {
        // 获取玩家朝向
        Vec3d lookVec = player.getRotationVec(1.0F);

        // 投弹点在玩家前方1.5米，下方1.0米
        Vec3d forward = new Vec3d(lookVec.x, 0, lookVec.z).normalize();
        Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0)).normalize();

        // 基础偏移
        Vec3d offset = forward.multiply(1.5).add(0, -1.0, 0);

        // 根据玩家运动状态调整
        Vec3d velocity = player.getVelocity();
        double speed = velocity.length();

        if (speed > 0.5) {
            // 高速时稍微靠前
            offset = offset.add(forward.multiply(speed * 0.2));
        }

        return offset;
    }

    /**
     * 应用惯性系数
     *
     * @param velocity 原始速度
     * @param config 配置参数
     * @return 应用系数后的速度
     */
    private static Vec3d applyInertiaCoefficients(Vec3d velocity, AdvancedPhysicsConfig config) {
        double horizontalInertia = config.horizontalInertia / 100.0;
        double verticalInertia = config.verticalInertia / 100.0;

        return new Vec3d(
                velocity.x * horizontalInertia,
                velocity.y * verticalInertia,
                velocity.z * horizontalInertia
        );
    }

    /**
     * 计算手动投掷力量
     *
     * @param player 玩家实体
     * @param config 配置参数
     * @return 投掷力量向量
     */
    private static Vec3d calculateThrowForce(PlayerEntity player, AdvancedPhysicsConfig config) {
        double throwForce = config.throwForce / 100.0 * 0.8; // 最大力量0.8 m/s

        // 投掷方向略微向下
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d throwDirection = new Vec3d(lookVec.x, lookVec.y - 0.2, lookVec.z).normalize();

        return throwDirection.multiply(throwForce);
    }

    /**
     * 计算空气阻力（增强模型）
     * 使用公式: F_drag = 0.5 * ρ * v² * Cd * A
     *
     * @param velocity 速度向量
     * @return 阻力向量
     */
    public static Vec3d calculateAirResistance(Vec3d velocity) {
        double speed = velocity.length();
        if (speed < 0.001) {
            return Vec3d.ZERO;
        }

        // 计算阻力大小
        double frontalArea = Math.PI * BOMB_RADIUS * BOMB_RADIUS; // 正面面积
        double dragMagnitude = 0.5 * AIR_DENSITY * speed * speed * DRAG_COEFFICIENT * frontalArea;

        // 阻力方向与速度方向相反
        Vec3d dragDirection = velocity.normalize().multiply(-1);

        // 计算阻力向量
        Vec3d dragForce = dragDirection.multiply(dragMagnitude);

        // 转换为加速度 (F = ma => a = F/m)
        return dragForce.multiply(1.0 / BOMB_MASS);
    }

    /**
     * 更新炸弹位置（考虑所有物理因素）
     *
     * @param motionState 运动状态
     * @param deltaTime 时间增量（秒）
     * @return 更新后的运动状态
     */
    public static BombMotionState updateMotion(BombMotionState motionState, double deltaTime) {
        // 应用重力
        Vec3d gravityAcceleration = new Vec3d(0, -GRAVITY, 0);

        // 计算空气阻力
        Vec3d dragAcceleration = calculateAirResistance(motionState.velocity);

        // 总加速度
        Vec3d totalAcceleration = gravityAcceleration.add(dragAcceleration);

        // 更新速度 (v = v0 + a*t)
        Vec3d newVelocity = motionState.velocity.add(totalAcceleration.multiply(deltaTime));

        // 更新位置 (s = s0 + v*t)
        Vec3d newPosition = motionState.position.add(motionState.velocity.multiply(deltaTime));

        // 更新角速度（简单的阻尼）
        Vec3d newAngularVelocity = motionState.angularVelocity.multiply(0.95);

        // 创建新的运动状态
        BombMotionState newState = new BombMotionState();
        newState.position = newPosition;
        newState.velocity = newVelocity;
        newState.angularVelocity = newAngularVelocity;

        return newState;
    }

    /**
     * 预测命中点
     *
     * @param player 玩家实体
     * @param config 配置参数
     * @param maxTime 预测最大时间（秒）
     * @param timeStep 时间步长（秒）
     * @return 预测的命中点
     */
    public static Vec3d predictImpactPoint(PlayerEntity player, AdvancedPhysicsConfig config,
                                           double maxTime, double timeStep) {
        // 获取初始运动状态
        BombMotionState motionState = calculateAdvancedMomentum(player, config);

        // 模拟飞行轨迹
        double time = 0;
        while (time < maxTime) {
            // 检查是否撞击地面
            if (motionState.position.y <= 0) {
                return motionState.position;
            }

            // 更新运动状态
            motionState = updateMotion(motionState, timeStep);
            time += timeStep;
        }

        return motionState.position;
    }
}
