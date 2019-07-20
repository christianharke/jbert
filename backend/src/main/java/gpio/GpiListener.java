package gpio;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import util.LogHelper;

import java.util.logging.Logger;


public class GpiListener implements AutoCloseable {
    private static final Logger logger = LogHelper.getLogger(GpiListener.class.getName());
    private final GpioController gpioController = GpioFactory.getInstance();
    private final GpioPinDigitalInput digitalInput;
    private final Pin pin;

    public GpiListener(Pin pin) {
        logger.finer(String.format("Configure pin '%s' as input", pin));
        this.pin = pin;
        digitalInput = gpioController.provisionDigitalInputPin(pin, PinPullResistance.PULL_DOWN);
        digitalInput.setShutdownOptions(true);
    }

    public void registerAction(GpioPinListenerDigital listener) {
        logger.fine(String.format("Registering listener '%s' for pin '%s'", listener, pin));
        digitalInput.addListener(listener);
    }

    @Override
    public void close() {
        gpioController.shutdown();
    }
}