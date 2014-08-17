package appstate;

import org.springframework.web.servlet.ModelAndView;

import de.comlineag.snc.appstate.RuntimeController;
import junit.framework.TestCase;

public class RuntimeControllerTests extends TestCase {
	
    public void testHandleRequestView() throws Exception{		
    	RuntimeController controller = new RuntimeController();
	    ModelAndView modelAndView = controller.handleRequest(null, null);		
	    assertEquals("index.jsp", modelAndView.getViewName());
    }
}
