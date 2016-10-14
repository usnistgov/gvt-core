//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2015.05.05 at 04:48:37 PM EDT
//

package gov.nist.hit.core.domain;

public enum TableType {
  HL_7("HL7"), USER("User"), LOCAL("Local"), EXTERNAL("External"), IMPORTED("Imported");
  private final String value;

  TableType(String v) {
    value = v;
  }

  public String value() {
    return value;
  }

  public static TableType fromValue(String v) {
    for (TableType c : TableType.values()) {
      if (c.value.equals(v)) {
        return c;
      }
    }
    throw new IllegalArgumentException(v);
  }

}
