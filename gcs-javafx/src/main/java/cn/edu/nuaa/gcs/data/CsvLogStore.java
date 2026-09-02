package cn.edu.nuaa.gcs.data;

import cn.edu.nuaa.gcs.model.LogRecord;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class CsvLogStore {
    private static final String HEADER = "id,timestamp,lat,lon,alt,speed,voltage,model,duration,distance";

    public static void backup(List<LogRecord> logs, File file) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            w.write(HEADER);
            w.newLine();
            for (LogRecord r : logs) {
                w.write(String.format("%d,%d,%.7f,%.7f,%.1f,%.1f,%.2f,%s,%d,%.1f",
                        r.getId(), r.getTimestamp(), r.getLat(), r.getLon(),
                        r.getAlt(), r.getSpeed(), r.getVoltage(), r.getDroneModel(),
                        r.getDuration(), r.getDistance()));
                w.newLine();
            }
        }
    }

    public static List<LogRecord> restore(File file) throws IOException {
        List<LogRecord> logs = new ArrayList<>();
        int skipped = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line = r.readLine();
            if (line == null) throw new IOException("CSV 文件为空");
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(",");
                if (cols.length < 7) { skipped++; continue; }
                try {
                    LogRecord rec = new LogRecord();
                    int idx = 0;
                    if (cols.length >= 10) {
                        rec.setId(Integer.parseInt(cols[idx++]));
                    }
                    rec.setTimestamp(Long.parseLong(cols[idx++]));
                    rec.setLat(Double.parseDouble(cols[idx++]));
                    rec.setLon(Double.parseDouble(cols[idx++]));
                    rec.setAlt(Double.parseDouble(cols[idx++]));
                    rec.setSpeed(Double.parseDouble(cols[idx++]));
                    rec.setVoltage(Double.parseDouble(cols[idx++]));
                    rec.setDroneModel(cols[idx++]);
                    if (cols.length >= 10) {
                        rec.setDuration(Integer.parseInt(cols[idx++]));
                        rec.setDistance(Double.parseDouble(cols[idx++]));
                    }
                    logs.add(rec);
                } catch (NumberFormatException e) {
                    skipped++;
                }
            }
        }
        if (skipped > 0)
            System.err.println("CSV 恢复: 成功 " + logs.size() + " 行，跳过 " + skipped + " 行");
        return logs;
    }

    public static List<LogRecord> filter(List<LogRecord> logs,
                                          Predicate<LogRecord> dateFilter,
                                          String modelFilter) {
        List<LogRecord> result = new ArrayList<>();
        for (LogRecord r : logs) {
            if (dateFilter != null && !dateFilter.test(r)) continue;
            if (modelFilter != null && !modelFilter.isEmpty() && !r.getDroneModel().equals(modelFilter)) continue;
            result.add(r);
        }
        return result;
    }
}
