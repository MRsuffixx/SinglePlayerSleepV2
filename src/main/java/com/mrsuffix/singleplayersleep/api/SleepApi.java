package com.mrsuffix.singleplayersleep.api;

import com.mrsuffix.singleplayersleep.api.events.*;

public interface SleepApi {

    void onNightSkip(NightSkipEvent event);

    void onPlayerSleep(PlayerSleepEvent event);

    void onPlayerWake(PlayerWakeEvent event);

    void onVoteCast(VoteCastEvent event);

    void onAfkStatusChange(AfkStatusChangeEvent event);
}