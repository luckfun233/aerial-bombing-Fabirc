package com.aerial.bombing.physics;

import net.minecraft.util.math.Vec3d;

/**
 * 炸弹运动状态数据类
 */
public class BombMotionState {
    public Vec3d position;      // 位置
    public Vec3d velocity;      // 线速度
    public Vec3d angularVelocity; // 角速度

    public BombMotionState() {
        this.position = Vec3d.ZERO;
        this.velocity = Vec3d.ZERO;
        this.angularVelocity = Vec3d.ZERO;
    }

    public BombMotionState(Vec3d position, Vec3d velocity, Vec3d angularVelocity) {
        this.position = position;
        this.velocity = velocity;
        this.angularVelocity = angularVelocity;
    }
}
