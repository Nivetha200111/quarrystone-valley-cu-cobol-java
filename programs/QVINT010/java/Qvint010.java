import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

public class Qvint010 {
    private static BufferedReader acctReader;
    private static BufferedWriter intWriter;
    private static boolean eof = false;
    private static String qaAcctId = "          ";
    private static BigDecimal qaBalance = BigDecimal.ZERO;
    private static BigDecimal qaRate = BigDecimal.ZERO;
    private static int qaDays = 0;
    private static char qaTier = ' ';
    private static BigDecimal wsRate = BigDecimal.ZERO;
    private static BigDecimal wsInt = BigDecimal.ZERO;
    private static final BigDecimal WS_OD_RATE = new BigDecimal("0.1800");
    private static final BigDecimal WS_BONUS = new BigDecimal("0.0025");
    private static final BigDecimal LIMIT_25000 = new BigDecimal("25000.00");
    private static String qiAcctId = "          ";
    private static BigDecimal qiInterest = BigDecimal.ZERO;
    private static BigDecimal qiNewBal = BigDecimal.ZERO;
    private static String qiFlag = "  ";

    public static void main(String[] args) throws Exception {
        m0000Main();
    }

    private static void m0000Main() throws Exception {
        String acctPath = System.getenv("DD_ACCTIN");
        String outPath = System.getenv("DD_INTOUT");
        acctReader = new BufferedReader(new InputStreamReader(new FileInputStream(acctPath), StandardCharsets.ISO_8859_1));
        intWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outPath), StandardCharsets.ISO_8859_1));
        eof = false;
        m8000Read();
        while (!eof) {
            m2000Accrue();
        }
        acctReader.close();
        intWriter.close();
    }

    private static void m2000Accrue() throws Exception {
        wsInt = BigDecimal.ZERO.setScale(2);
        qiFlag = "ZR";
        if (qaBalance.compareTo(BigDecimal.ZERO) > 0) {
            wsRate = truncateToPic(qaRate, 1, 6);
            if (qaTier == 'P' && qaBalance.compareTo(LIMIT_25000) >= 0) {
                wsRate = truncateToPic(wsRate.add(WS_BONUS), 1, 6);
            }
            BigDecimal numerator = qaBalance.multiply(wsRate).multiply(BigDecimal.valueOf((long) qaDays));
            BigDecimal computed = numerator.divide(BigDecimal.valueOf(365L), 2, RoundingMode.HALF_UP);
            wsInt = truncateToPic(computed, 7, 2);
            qiFlag = "CR";
        } else if (qaBalance.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal numerator = qaBalance.multiply(WS_OD_RATE).multiply(BigDecimal.valueOf((long) qaDays));
            BigDecimal computed = numerator.divide(BigDecimal.valueOf(365L), 2, RoundingMode.DOWN);
            wsInt = truncateToPic(computed, 7, 2);
            qiFlag = "OD";
        } else {
            wsInt = BigDecimal.ZERO.setScale(2);
            qiFlag = "ZR";
        }
        qiAcctId = qaAcctId;
        qiInterest = wsInt;
        BigDecimal newBalRaw = qaBalance.add(wsInt);
        qiNewBal = truncateToPic(newBalRaw, 9, 2);
        String interestStr = formatSignedSeparate(qiInterest, 7, 2);
        String newBalStr = formatSignedSeparate(qiNewBal, 9, 2);
        StringBuilder rec = new StringBuilder(34);
        rec.append(qiAcctId);
        rec.append(interestStr);
        rec.append(newBalStr);
        rec.append(qiFlag);
        intWriter.write(rec.toString());
        intWriter.write("\n");
        m8000Read();
    }

    private static void m8000Read() throws Exception {
        String line = acctReader.readLine();
        if (line == null) {
            eof = true;
            return;
        }
        if (line.length() < 33) {
            StringBuilder sb = new StringBuilder(line);
            while (sb.length() < 33) {
                sb.append(' ');
            }
            line = sb.toString();
        } else if (line.length() > 33) {
            line = line.substring(0, 33);
        }
        qaAcctId = line.substring(0, 10);
        String balStr = line.substring(10, 22);
        String rateStr = line.substring(22, 27);
        String daysStr = line.substring(27, 30);
        qaTier = line.charAt(30);
        qaBalance = parseSignedSeparate(balStr, 9, 2);
        qaRate = parseUnsigned(rateStr, 1, 4);
        qaDays = parseDays(daysStr);
        eof = false;
    }

    private static BigDecimal parseSignedSeparate(String s, int intDigits, int fracDigits) {
        char sc = s.charAt(0);
        int sign = (sc == '-') ? -1 : 1;
        BigInteger acc = BigInteger.ZERO;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = (c & 0x0F);
            acc = acc.multiply(BigInteger.TEN).add(BigInteger.valueOf(d));
        }
        BigDecimal v = new BigDecimal(acc).movePointLeft(fracDigits);
        v = v.setScale(fracDigits, RoundingMode.UNNECESSARY);
        if (sign < 0) {
            v = v.negate();
        }
        return v;
    }

    private static BigDecimal parseUnsigned(String s, int intDigits, int fracDigits) {
        BigInteger acc = BigInteger.ZERO;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = (c & 0x0F);
            acc = acc.multiply(BigInteger.TEN).add(BigInteger.valueOf(d));
        }
        BigDecimal v = new BigDecimal(acc).movePointLeft(fracDigits);
        return v.setScale(fracDigits, RoundingMode.UNNECESSARY);
    }

    private static int parseDays(String s) {
        BigInteger acc = BigInteger.ZERO;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = (c & 0x0F);
            acc = acc.multiply(BigInteger.TEN).add(BigInteger.valueOf(d));
        }
        acc = acc.remainder(BigInteger.valueOf(1000L));
        return acc.intValue();
    }

    private static BigDecimal truncateToPic(BigDecimal v, int intDigits, int fracDigits) {
        BigDecimal scaled = v.setScale(fracDigits, RoundingMode.UNNECESSARY).movePointRight(fracDigits);
        BigInteger bi = scaled.toBigIntegerExact();
        BigInteger mod = BigInteger.TEN.pow(intDigits + fracDigits);
        boolean neg = bi.signum() < 0;
        BigInteger abs = bi.abs().remainder(mod);
        BigInteger res = neg ? abs.negate() : abs;
        return new BigDecimal(res).movePointLeft(fracDigits).setScale(fracDigits, RoundingMode.UNNECESSARY);
    }

    private static String formatSignedSeparate(BigDecimal v, int intDigits, int fracDigits) {
        BigDecimal sv = v.setScale(fracDigits, RoundingMode.UNNECESSARY);
        boolean neg = sv.signum() < 0;
        BigInteger scaled = sv.movePointRight(fracDigits).toBigIntegerExact().abs();
        String d = scaled.toString();
        int total = intDigits + fracDigits;
        while (d.length() < total) {
            d = "0" + d;
        }
        if (d.length() > total) {
            d = d.substring(d.length() - total);
        }
        return (neg ? "-" : "+") + d;
    }
}
