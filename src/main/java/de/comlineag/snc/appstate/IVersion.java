package de.comlineag.snc.appstate;

import de.comlineag.snc.data.Version;

/**
 * 
 * @author		Christian Günther
 * @type		Interface
 * @version		0.1			- 14.11.2014
 * @status		productive but rarely used
 * @description	Interface that those SNC components that are explicitly versioned will implement.
 * 				Intention is to allow both plug-in components (custom extensions) and applications and
 * 				frameworks that use SNC to detect exact version of SNC in use.
 * 				This may be useful for example for ensuring that proper SNC version is deployed
 * 
 * @since 0.9
 */
public interface IVersion {
	    /**
	     * Method called to detect version of the component that implements this interface;
	     * returned version should never be null, but may return specific "not available"
	     * instance (see {@link Version} for details).
	     */
	    public Version version();
}
