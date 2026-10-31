import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Qvfee020 {
    private static String acctPath;
    private static String feePath;
    private static BufferedReader acctReader;
    private static BufferedWriter feeWriter;
    private static boolean endOfFile = false;
    private static String qaAcctId = "";
    private static BigDecimal qaBalance = BigDecimal.ZERO.setScale(2);
    private static String qaTier = " ";
    private static int qaNsfCount = 0;
    private static final BigDecimal WS_ITEM_FEE = new BigDecimal("29.00");
    private static final BigDecimal WS_DAILY_CAP = new BigDecimal("87.00");
    private static final BigDecimal NEG_TEN = new BigDecimal("-10.00");
    private static final BigDecimal HALF_PCT = new BigDecimal("0.005");
    private static final BigDecimal MOD_BASE = new BigDecimal("10000000");
    private static BigDecimal wsFee = BigDecimal.ZERO.setScale(2);
    private static String qfAcctId = "";
    private static String qfReason = "NONE";
    public static void main(String[] args) throws Exception {
        acctPath = System.getenv("DD_ACCTIN");
        feePath = System.getenv("DD_FEEOUT");
        String clock = System.getenv("DIOCLE_CLOCK");
        String collation = System.getenv("DIOCLE_COLLATION");
        if (clock != null && clock.isEmpty()) {
        }
        if (collation != null && collation.isEmpty()) {
        }
        p0000Main();
    }
    private static void p0000Main() throws Exception {
        acctReader = Files.newBufferedReader(Paths.get(acctPath), StandardCharsets.ISO_8859_1);
        feeWriter = Files.newBufferedWriter(Paths.get(feePath), StandardCharsets.ISO_8859_1, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        endOfFile = false;
        p8000Read();
        while (!endOfFile) {
            p2000Assess();
        }
        acctReader.close();
        feeWriter.close();
    }
    private static void p2000Assess() throws Exception {
        wsFee = BigDecimal.ZERO.setScale(2);
        qfReason = "NONE";
        if ("P".equals(qaTier)) {
            qfReason = "WPRM";
        } else if (qaBalance.compareTo(NEG_TEN) >= 0 && qaNsfCount == 0) {
        } else if ("S".equals(qaTier) && qaNsfCount <= 2) {
            qfReason = "WSTU";
        } else {
            p3000Calc();
        }
        qfAcctId = qaAcctId;
        String feeField = formatSignedSeparate(wsFee, 5, 2);
        String outLine = padRight(qfAcctId, 10) + feeField + padRight(qfReason, 4);
        feeWriter.write(outLine);
        feeWriter.write("\n");
        p8000Read();
    }
    private static void p3000Calc() {
        BigDecimal raw = BigDecimal.valueOf((long) qaNsfCount).multiply(WS_ITEM_FEE);
        wsFee = storeWsFee(raw);
        if (qaBalance.compareTo(NEG_TEN) < 0) {
            BigDecimal prod = qaBalance.multiply(HALF_PCT);
            BigDecimal raw2 = wsFee.subtract(prod);
            wsFee = storeWsFee(raw2);
        }
        if (wsFee.compareTo(WS_DAILY_CAP) > 0) {
            wsFee = WS_DAILY_CAP;
            qfReason = "CAPD";
        } else {
            qfReason = "CHRG";
        }
    }
    private static void p8000Read() throws Exception {
        String line = acctReader.readLine();
        if (line == null) {
            endOfFile = true;
            return;
        }
        if (line.length() < 33) {
            line = padRight(line, 33);
        } else if (line.length() > 33) {
            line = line.substring(0, 33);
        }
        qaAcctId = line.substring(0, 10);
        String balField = line.substring(10, 22);
        qaTier = line.substring(30, 31);
        String nsfField = line.substring(31, 33);
        qaBalance = parseSignedSeparate(balField, 2);
        qaNsfCount = parseUnsigned(nsfField);
    }
    private static BigDecimal parseSignedSeparate(String field, int scale) {
        boolean neg = !field.isEmpty() && field.charAt(0) == '-';
        String dig = field.length() > 1 ? field.substring(1) : "";
        StringBuilder sb = new StringBuilder(dig.length());
        for (int i = 0; i < dig.length(); i++) {
            char c = dig.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else {
                sb.append('0');
            }
        }
        String d = sb.length() == 0 ? "0" : sb.toString();
        BigDecimal abs = new BigDecimal(d).movePointLeft(scale).setScale(scale);
        return neg ? abs.negate() : abs;
    }
    private static int parseUnsigned(String field) {
        StringBuilder sb = new StringBuilder(field.length());
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else {
                sb.append('0');
            }
        }
        String d = sb.toString();
        if (d.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(d);
    }
    private static BigDecimal storeWsFee(BigDecimal v) {
        BigDecimal t = v.setScale(2, RoundingMode.DOWN);
        BigDecimal cents = t.movePointRight(2);
        BigDecimal r = cents.remainder(MOD_BASE);
        BigDecimal res = r.movePointLeft(2).setScale(2, RoundingMode.DOWN);
        if (res.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return res;
    }
    private static String formatSignedSeparate(BigDecimal val, int intDigits, int fracDigits) {
        boolean neg = val.signum() < 0;
        BigDecimal abs = val.abs();
        BigDecimal cents = abs.movePointRight(fracDigits).setScale(0, RoundingMode.DOWN);
        String digits = cents.toPlainString();
        int total = intDigits + fracDigits;
        if (digits.length() > total) {
            digits = digits.substring(digits.length() - total);
        } else if (digits.length() < total) {
            StringBuilder sb = new StringBuilder(total);
            for (int i = 0; i < total - digits.length(); i++) {
                sb.append('0');
            }
            sb.append(digits);
            digits = sb.toString();
        }
        return (neg ? "-" : "+") + digits;
    }
    private static String padRight(String s, int n) {
        if (s.length() >= n) {
            return s.substring(0, n);
        }
        StringBuilder sb = new StringBuilder(n);
        sb.append(s);
        for (int i = s.length(); i < n; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
