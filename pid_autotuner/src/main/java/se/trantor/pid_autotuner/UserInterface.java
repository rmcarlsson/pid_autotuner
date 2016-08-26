package se.trantor.pid_autotuner;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserInterface implements Runnable {
	
	public HeaterController heater;
	public int StartPower = 150;
	public int Step = 100;
	private static final Logger logger = Logger.getLogger(Heater.class.getName());
	
	
	public UserInterface(HeaterController aHeater)
	{
		heater = aHeater;
	}
	
	
	public void run() {

		boolean go = true;
		heater.SetPower(StartPower);

		while (go) {
			String cmd = System.console().readLine();
			go = handleCmd(cmd);
		}

	}

	protected boolean handleCmd(String aCmd) {
		
		String patternString = "[a-z]+([0-9]+)";
		
		if (aCmd.matches("bye"))
			return false;


		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(aCmd);
		int val = 0;
		if (matcher.find())
		{
			try {
				val = Integer.parseInt(matcher.group(1));
				if (aCmd.matches("power.*"))
				{
					heater.SetPower(val);
					StartPower = val;
				}
				if (aCmd.matches("step.*"))
				{
					Step = val;
					heater.SetPower(StartPower + Step);
				}
			}
			catch (NumberFormatException e)
			{
				logger.log(Level.FINE, "Unable to parse string {0}", aCmd);
			}
			
		}
		// TODO Auto-generated method stub
		return true;
	}
}
