/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package xyz.redtorch.gateway.ctp.x64v6v3v19t1v.api;

public class CThostFtdcLoginForbiddenIPField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcLoginForbiddenIPField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcLoginForbiddenIPField obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        jctpv6v3v19t1x64apiJNI.delete_CThostFtdcLoginForbiddenIPField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setIPAddress(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcLoginForbiddenIPField_IPAddress_set(swigCPtr, this, value);
  }

  public String getIPAddress() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcLoginForbiddenIPField_IPAddress_get(swigCPtr, this);
  }

  public CThostFtdcLoginForbiddenIPField() {
    this(jctpv6v3v19t1x64apiJNI.new_CThostFtdcLoginForbiddenIPField(), true);
  }

}
