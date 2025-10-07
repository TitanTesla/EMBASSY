package util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class NumberUtil {
  public static String price(BigDecimal v) {
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    nf.setMinimumFractionDigits(2);
    nf.setMaximumFractionDigits(2);
    return nf.format(v);
  }
}
