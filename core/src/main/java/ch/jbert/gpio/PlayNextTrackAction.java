package ch.jbert.gpio;

import ch.jbert.mpd.MpdService;
import com.pi4j.io.gpio.PinEdge;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;


public class PlayNextTrackAction extends DebouncedGpiAction {
    private final MpdService mpdService;

    public PlayNextTrackAction(MpdService mpdService) {
        super(PinEdge.RISING);
        this.mpdService = mpdService;

    }

    @Override
    public void gpiEventAction(GpioPinDigitalStateChangeEvent event) {
        mpdService.playNext();
    }
}
