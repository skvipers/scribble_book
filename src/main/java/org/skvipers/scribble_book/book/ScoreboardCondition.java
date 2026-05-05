package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;

public record ScoreboardCondition(String objective, int min) implements UnlockCondition {

    public static final MapCodec<ScoreboardCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    Codec.STRING.fieldOf("objective").forGetter(ScoreboardCondition::objective),
                    Codec.INT.optionalFieldOf("min", 1).forGetter(ScoreboardCondition::min)
            ).apply(i, ScoreboardCondition::new));

    @Override
    public String type() { return "scoreboard"; }

    @Override
    public boolean isMet(BookData bookData, ServerPlayer player) {
        var scoreboard = player.level().getServer().getScoreboard();
        Objective obj = scoreboard.getObjective(objective);
        if (obj == null) return false;
        ReadOnlyScoreInfo info = scoreboard.getPlayerScoreInfo(player, obj);
        return info != null && info.value() >= min;
    }
}
