package se.trantor.pid_autotuner;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HeaterControllerStub implements HeaterController {
	
	public int Power;

	private static final Logger logger = Logger.getLogger(HeaterControllerStub.class.getName());
	
	public void SetPower(int aPower) {
		Power = aPower;
		logger.log(Level.FINE, "Set power to {0}", aPower);
	}

	
	public int GetPower()
	{
		return Power;
	}
}
