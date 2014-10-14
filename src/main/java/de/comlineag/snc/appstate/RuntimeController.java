package de.comlineag.snc.appstate;

import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author		Christian Guenther
 * @category	controller
 * @revision	0.1
 * @status		in development
 * 
 * @description	This is a very basic Controller implementation. The Controller handles 
 * 				the request and returns a ModelAndView - in this case, one named 'index.jsp' 
 * 				The model that this class returns is actually resolved via a ViewResolver. 
 * 				Since we have not explicitly defined a ViewResolver, we are going to be given 
 * 				a default one by Spring that simply forwards to a URL matching the name of 
 * 				the view specified.
 *
 * @changelog	0.1 (Chris)		class created
 * 
 */
//@Controller
public class RuntimeController implements Controller {
	protected final Log logger = LogFactory.getLog(getClass());
	
	//@RequestMapping("/admin")
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		logger.info("Returning index.jsp");
		return new ModelAndView("index.jsp");
		
	}
}
