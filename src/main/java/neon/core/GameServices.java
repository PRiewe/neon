package neon.core;

import neon.systems.physics.PhysicsSystem;

public record GameServices(PhysicsSystem physicsEngine, ScriptEngine scriptEngine) {}
