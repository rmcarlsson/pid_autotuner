package se.trantor.pid_autotuner;

/**
 * Hello world!
 *
 */
public class App  
{
	
	 
	
    public static void main( String[] args )
    {
    	
   	
    	Heater heater = new Heater();
    	HeaterController hc = heater;
        Thread hcThread = new Thread(heater);
        hcThread.start();
        
        Temperature t = new Temperature();
        Runnable r1 = t;
        Thread tThread = new Thread(r1);
        tThread.start();
        
        UserInterface ui = new UserInterface(hc);
        Thread uiThread = new Thread(ui);
        uiThread.start();
        
        try {
			uiThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        PidATune pidftuner =  new PidATune(500, t, hc, ui.StartPower, ui.Step);
        Thread pidtunerThread = new Thread(pidftuner);
        pidtunerThread.start();
        
        try {
        	pidtunerThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        

        
        
    }
}
