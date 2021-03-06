package se.trantor.pid_autotuner;


import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class Heater implements Runnable, HeaterController {

	private int power = 0;

	private static final Logger logger = Logger.getLogger(Heater.class.getName());
	public static final int PERIOD = 5000;
	public static final int MAX_POWER = 2200;


	public void SetPower(int controlSignal) {
		power = controlSignal;
		logger.log(Level.INFO, "Set power to {0}", controlSignal);

	}

	public int getPower() {
		return power;
	}

	public void run() {

		logger.log(Level.INFO, "Spawning heater");
		
		// create gpio controller
        final GpioController gpio = GpioFactory.getInstance();
        
        // provision gpio pin #01 as an output pin and turn on
        final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "Heater", PinState.LOW);
        
		//gpio_write_val (0);
		try {

			while (true)
			{
				long on_time = Heater.PERIOD;
				
				if (power == 0)
					on_time = 0;
				if (power < (int)Math.abs((double)MAX_POWER * 0.95))
					on_time = (power * PERIOD) / MAX_POWER;
				else
					on_time = PERIOD;

				logger.log(Level.FINE, MessageFormat.format("Power is {0}, on time is {1}", power, on_time));
				
				long off_time = PERIOD - on_time;

				if (on_time > 0)
				{
					pin.high();
					Thread.sleep(on_time);
					
				}

				if (off_time > 0) 
				{
					pin.low();
					Thread.sleep(off_time);
				}
			}
		} 
		catch (InterruptedException e) 
		{
			pin.low();
		}	
	}
}
