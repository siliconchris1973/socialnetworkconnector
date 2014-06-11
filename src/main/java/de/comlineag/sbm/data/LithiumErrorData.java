package de.comlineag.sbm.data;

public class LithiumErrorData {

	public LithiumErrorData() {}
	
	/*
	 * This is the structure of an Lithium Error Response
		<response status="error">
			<error code="504">
				<message>
					Methode ???get??? wird nicht au??erhalb von Knoten ???community.messages??? unterst??tzt
 				</message>
  			</error>
		</response>
	 */
	
	// XML attribute id
	private int id;
	// XML element error
	private String responseStatus;
	private String errorCode;
	private String message;
 
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getResponseStatus() {
		return responseStatus;
	}
    public void setResponseStatus(String responseStatus) {
		this.responseStatus = responseStatus;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
    
    @Override
    public String toString() {
        return this.id + ":" + this.errorCode;
    }
	
	
}
