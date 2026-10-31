import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Qvlon030 {
    private BufferedReader loanReader;
    private BufferedWriter lateWriter;
    private boolean endOfFile = false;
    private int wsGrace = 0;
    private BigDecimal wsCap = BigDecimal.ZERO;
    private BigDecimal wsFee = BigDecimal.ZERO;
    private String qlLoanId = "";
    private BigDecimal qlPaymentDue = BigDecimal.ZERO;
    private int qlDaysLate = 0;
    private int qlPriorLate = 0;
    private String qlType = " ";
    private String qtLoanId = "";
    private BigDecimal qtLateFee = BigDecimal.ZERO;
    private String qtGrace = " ";
    private String qtCode = "    ";

    public static void main(String[] args) throws Exception {
        Qvlon030 p = new Qvlon030();
        p.main0000();
    }

    private void main0000() throws Exception {
        String loanIn = System.getenv("DD_LOANIN");
        String lateOut = System.getenv("DD_LATEOUT");
        Path inPath = Paths.get(loanIn);
        Path outPath = Paths.get(lateOut);
        loanReader = Files.newBufferedReader(inPath, StandardCharsets.ISO_8859_1);
        lateWriter = Files.newBufferedWriter(outPath, StandardCharsets.ISO_8859_1);
        try {
            read8000();
            while (!endOfFile) {
                assess2000();
            }
        } finally {
            try { loanReader.close(); } catch (IOException e) { }
            try { lateWriter.close(); } catch (IOException e) { }
        }
    }

    private void assess2000() throws Exception {
        wsFee = BigDecimal.ZERO.setScale(2);
        boolean isMortgage = qlType.equals("M");
        boolean isCard = qlType.equals("C");
        if (isMortgage) {
            wsGrace = 15;
            wsCap = new BigDecimal("150.00");
        } else {
            wsGrace = 10;
            wsCap = new BigDecimal("40.00");
        }
        if (qlDaysLate <= wsGrace) {
            qtGrace = "Y";
            qtCode = "GRCE";
        } else {
            qtGrace = "N";
            qtCode = "LATE";
            BigDecimal fee = qlPaymentDue.multiply(new BigDecimal("0.05"));
            fee = fee.setScale(2, RoundingMode.HALF_UP);
            wsFee = truncFee(fee);
            if (isCard && wsFee.compareTo(new BigDecimal("15.00")) < 0) {
                wsFee = truncFee(new BigDecimal("15.00"));
            }
            if (qlPriorLate >= 3) {
                wsFee = truncFee(wsFee.add(new BigDecimal("10.00")));
                wsFee = wsFee.setScale(2, RoundingMode.UNNECESSARY);
                qtCode = "REPT";
            }
            if (wsFee.compareTo(wsCap) > 0) {
                wsFee = wsCap;
                qtCode = "CAPD";
            }
        }
        qtLoanId = qlLoanId;
        qtLateFee = wsFee;
        String outRec = buildLateRecord();
        lateWriter.write(outRec);
        lateWriter.write("\n");
        read8000();
    }

    private void read8000() throws Exception {
        String line = loanReader.readLine();
        if (line == null) {
            endOfFile = true;
        } else {
            if (line.length() < 25) {
                StringBuilder sb = new StringBuilder(line);
                while (sb.length() < 25) sb.append(' ');
                line = sb.toString();
            } else if (line.length() > 25) {
                line = line.substring(0, 25);
            }
            qlLoanId = line.substring(0, 10);
            String payStr = line.substring(10, 19);
            String daysStr = line.substring(19, 22);
            String priorStr = line.substring(22, 24);
            qlType = line.substring(24, 25);
            qlPaymentDue = parsePayment(payStr);
            qlDaysLate = parseIntField(daysStr);
            qlPriorLate = parseIntField(priorStr);
        }
    }

    private static BigDecimal parsePayment(String f) {
        if (f.trim().isEmpty()) return BigDecimal.ZERO.setScale(2);
        String t = f.trim();
        if (t.contains(".")) {
            BigDecimal v = new BigDecimal(t);
            return v.setScale(2, RoundingMode.UNNECESSARY);
        }
        String s = t;
        boolean neg = false;
        if (s.startsWith("-") || s.startsWith("+")) {
            if (s.startsWith("-")) neg = true;
            s = s.substring(1);
        }
        if (s.isEmpty()) return BigDecimal.ZERO.setScale(2);
        BigDecimal v = new BigDecimal(s).movePointLeft(2);
        v = v.setScale(2, RoundingMode.UNNECESSARY);
        if (neg) v = v.negate();
        return v;
    }

    private static int parseIntField(String f) {
        if (f.trim().isEmpty()) return 0;
        String t = f.trim();
        return Integer.parseInt(t);
    }

    private static BigDecimal truncFee(BigDecimal v) {
        BigDecimal c = v.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2);
        long cents = c.longValueExact() % 10000000L;
        if (cents < 0) cents += 10000000L;
        return BigDecimal.valueOf(cents).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY);
    }

    private String buildLateRecord() {
        String loanId = qtLoanId;
        if (loanId.length() < 10) {
            StringBuilder sb = new StringBuilder(loanId);
            while (sb.length() < 10) sb.append(' ');
            loanId = sb.toString();
        } else if (loanId.length() > 10) {
            loanId = loanId.substring(0, 10);
        }
        BigDecimal fee = qtLateFee.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal centsBd = fee.movePointRight(2);
        long cents = centsBd.longValueExact();
        long mod = cents % 10000000L;
        if (mod < 0) mod += 10000000L;
        String feeStr = String.format("%07d", mod);
        return loanId + feeStr + qtGrace + qtCode;
    }
}
