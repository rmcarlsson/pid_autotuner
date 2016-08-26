package se.trantor.pid_autotuner;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pi4j.component.temperature.TemperatureSensor;
import com.pi4j.component.temperature.impl.TmpDS18B20DeviceType;
import com.pi4j.io.w1.W1Device;
import com.pi4j.io.w1.W1Master;



public class Temperature implements TemperatureProvider, Runnable {

	
	double temperature;
	double logTemperature;
	private static final Logger logger = Logger.getLogger(App.class.getName());
	private W1Device device = null;


	public double GetTemperature()
	{
		logger.log(Level.FINE, "Temperature is {0}", temperature);
		return temperature;
	}


	public void run() {
		logger.log(Level.INFO, "Spawning temperature");

		if (device == null)
		{
			W1Master master = new W1Master();
			List<W1Device> w1Devices = master.getDevices(TmpDS18B20DeviceType.FAMILY_CODE);
			for (W1Device device : w1Devices) 
			{
				logger.log(Level.INFO, "Found a 1-Wire temperature sensor, ID: {0}", device.getId());	            //returns the ID of the Sensor and the  full text of the virtual file
				this.device = device; 
			}
		}

		
		try {
			while (!Thread.currentThread().isInterrupted())
			{
				temperature = ((TemperatureSensor) device).getTemperature();
				if (Math.abs(logTemperature - temperature) > 0.4)
				{
					logger.log(Level.INFO, "Temperature is {0}", ((TemperatureSensor) device).getTemperature());
					logTemperature = temperature;
				}
				Thread.sleep(TimeUnit.SECONDS.toMillis(2));
			}
		 } catch (InterruptedException ex) {}
		 
	}
	
}
