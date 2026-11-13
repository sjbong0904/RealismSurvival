package com.example.realismsurvival.api.event;

import com.example.realismsurvival.api.environment.EnvironmentContext;
import com.example.realismsurvival.component.SurvivalData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Surrounds one survival evaluation window (thirst + temperature + future meters).
 *
 * <p>{@link Pre} — cancel to skip the whole window; mutate {@link Pre#setData} for early changes.
 * {@link Post} — observe final data (already written if changed).
 */
public abstract class SurvivalEvaluateEvent extends PlayerEvent {

    private final ServerLevel level;
    private final EnvironmentContext environment;
    private final float windowSeconds;

    protected SurvivalEvaluateEvent(
            ServerPlayer player,
            ServerLevel level,
            EnvironmentContext environment,
            float windowSeconds
    ) {
        super(player);
        this.level = level;
        this.environment = environment;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public ServerLevel getLevel() {
        return level;
    }

    public EnvironmentContext getEnvironment() {
        return environment;
    }

    public float getWindowSeconds() {
        return windowSeconds;
    }

    /** Before thirst/temperature. Cancel skips both (and dehydration damage this window). */
    public static final class Pre extends SurvivalEvaluateEvent implements ICancellableEvent {

        private SurvivalData data;

        public Pre(
                ServerPlayer player,
                ServerLevel level,
                EnvironmentContext environment,
                float windowSeconds,
                SurvivalData data
        ) {
            super(player, level, environment, windowSeconds);
            this.data = data;
        }

        public SurvivalData getData() {
            return data;
        }

        public void setData(SurvivalData data) {
            this.data = data;
        }
    }

    /** After thirst/temperature; {@link #getData()} is the value that was (or will be) stored. */
    public static final class Post extends SurvivalEvaluateEvent {

        private final SurvivalData previousData;
        private final SurvivalData data;
        private final boolean changed;

        public Post(
                ServerPlayer player,
                ServerLevel level,
                EnvironmentContext environment,
                float windowSeconds,
                SurvivalData previousData,
                SurvivalData data,
                boolean changed
        ) {
            super(player, level, environment, windowSeconds);
            this.previousData = previousData;
            this.data = data;
            this.changed = changed;
        }

        public SurvivalData getPreviousData() {
            return previousData;
        }

        public SurvivalData getData() {
            return data;
        }

        public boolean wasChanged() {
            return changed;
        }
    }
}
