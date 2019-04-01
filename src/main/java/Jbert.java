import com.pi4j.io.gpio.PinEdge;
import com.pi4j.io.gpio.RaspiPin;
import gpio.GpiListener;
import gpio.PlayPauseAction;
import gpio.VolumeDownAction;
import gpio.VolumeUpAction;
import mpd.MpdCommunicator;
import util.LogHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


class Jbert {
    private static final Logger logger = LogHelper.getLogger(Jbert.class.getName());

    private final MpdCommunicator mpdCommunicator;
    private List<GpiListener> gpiListeners;

    private Jbert(String server, int port) {
        mpdCommunicator = new MpdCommunicator(server, port);
    }

    private void start() throws InterruptedException {
        logger.info("Starting main jbert application...");

        mpdCommunicator.configure();
        gpiListeners = configureGpiListener();

        loopForever();
    }

    private List<GpiListener> configureGpiListener() {
        List<GpiListener> gpiListenerList = new ArrayList<>();

        GpiListener playPauseGpiListener = new GpiListener(RaspiPin.GPIO_03);
        playPauseGpiListener.registerAction(new PlayPauseAction(PinEdge.RISING, mpdCommunicator));
        gpiListenerList.add(playPauseGpiListener);

        GpiListener volumeUpGpiListener = new GpiListener(RaspiPin.GPIO_04);
        volumeUpGpiListener.registerAction(new VolumeUpAction(PinEdge.RISING, mpdCommunicator));
        gpiListenerList.add(volumeUpGpiListener);

        GpiListener volumeDownGpiListener = new GpiListener(RaspiPin.GPIO_05);
        volumeDownGpiListener.registerAction(new VolumeDownAction(PinEdge.RISING, mpdCommunicator));
        gpiListenerList.add(volumeDownGpiListener);

        return gpiListenerList;
    }

    private void loopForever() throws InterruptedException {
        logger.finer("Enter main loop...");

        while (true) {
            Thread.sleep(1000);
        }
    }

    public static void main(String... args) throws InterruptedException {
        Jbert jbert = new Jbert("localhost", 6600);
        jbert.start();
    }
}
