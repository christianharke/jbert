package gpio;

import com.pi4j.io.gpio.PinEdge;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import mpd.MpdCommunicator;


public class PlayNextTrackAction extends DebouncedGpiAction {
    private final MpdCommunicator mpdCommunicator;

    public PlayNextTrackAction(PinEdge edge, MpdCommunicator mpdCommunicator) {
        super(edge);
        this.mpdCommunicator = mpdCommunicator;

    }

    @Override
    public void gpiEventAction(GpioPinDigitalStateChangeEvent event) {
        mpdCommunicator.playNext();
    }
}