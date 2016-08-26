package se.trantor.pid_autotuner;

import junit.framework.TestCase;

public class UserInterfaceTest extends TestCase {

	public final void testHandleCmd() {
		
		
		HeaterControllerStub hStub = new HeaterControllerStub();
		UserInterface dut = new UserInterface(hStub);
		 
		
		assertTrue(dut.handleCmd("power90"));
		assertEquals(90, hStub.GetPower());
		
		assertTrue(dut.handleCmd("step90"));
		assertEquals((90+90), hStub.GetPower());
		
		assertFalse(dut.handleCmd("bye"));



	}

}


