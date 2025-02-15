package de.maxhenkel.voicechat.api.audiochannel;

import de.maxhenkel.voicechat.api.Position;

public interface LocationalAudioChannel extends AudioChannel {

    /**
     * Updates the location of the audio.
     *
     * @param position the audio location
     */
    void updateLocation(Position position);

    /**
     * @return the current location of this channel
     */
    Position getLocation();

}
