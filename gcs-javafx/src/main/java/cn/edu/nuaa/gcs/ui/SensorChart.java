package cn.edu.nuaa.gcs.ui;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.util.*;

public class SensorChart {
    private final LineChart<Number, Number> chart;
    private final NumberAxis xAxis;
    private final Map<String, XYChart.Series<Number, Number>> series = new LinkedHashMap<>();
    private final Map<String, Double> lastValue = new HashMap<>();
    private int sampleCount = 0;
    private int timeWindow = 30;
    private boolean paused = false;
    private final Map<String, String> channelColors = new LinkedHashMap<>();
    private final Set<String> enabledChannels = new HashSet<>();

    public SensorChart() {
        xAxis = new NumberAxis(0, timeWindow, 5);
        xAxis.setLabel("时间 (s)");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("值");
        chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setTitle("传感器数据曲线");
        chart.setPrefSize(800, 300);

        channelColors.put("温度", "#fb7185");
        channelColors.put("湿度", "#34d399");
        channelColors.put("气压", "#22d3ee");
        channelColors.put("横滚", "#fbbf24");
        channelColors.put("俯仰", "#a78bfa");
        channelColors.put("偏航", "#f472b6");
        channelColors.put("高度", "#60a5fa");
        channelColors.put("电压", "#ef4444");
        channelColors.put("电量", "#22c55e");

        enabledChannels.addAll(channelColors.keySet());
        for (String name : channelColors.keySet()) {
            XYChart.Series<Number, Number> s = new XYChart.Series<>();
            s.setName(name);
            series.put(name, s);
            chart.getData().add(s);
        }
    }

    public LineChart<Number, Number> getNode() { return chart; }

    public void push(Map<String, Double> values) {
        if (paused) return;
        sampleCount++;
        double t = sampleCount * 0.2;
        for (Map.Entry<String, Double> e : values.entrySet()) {
            String name = e.getKey();
            XYChart.Series<Number, Number> s = series.get(name);
            if (s == null) continue;
            // Always add data (even for disabled channels) to preserve history
            s.getData().add(new XYChart.Data<>(t, e.getValue()));
            while (s.getData().size() > timeWindow * 5)
                s.getData().remove(0);
        }
        // Update X axis to scroll with time (only if there are visible channels)
        if (!enabledChannels.isEmpty()) {
            while (xAxis.getUpperBound() < t) {
                xAxis.setLowerBound(xAxis.getLowerBound() + timeWindow);
                xAxis.setUpperBound(xAxis.getUpperBound() + timeWindow);
            }
        }
    }

    public void togglePause() { paused = !paused; }
    public void clear() {
        sampleCount = 0;
        series.values().forEach(s -> s.getData().clear());
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(timeWindow);
    }

    public void setTimeWindow(int seconds) {
        this.timeWindow = seconds;
        // Adjust X axis to show the latest data within the new window
        double currentTime = sampleCount * 0.2;
        if (currentTime > seconds) {
            xAxis.setLowerBound(currentTime - seconds);
            xAxis.setUpperBound(currentTime);
        } else {
            xAxis.setLowerBound(0);
            xAxis.setUpperBound(seconds);
        }
        xAxis.setTickUnit(seconds / 6.0);
        // Trim data to fit new window
        int maxSamples = seconds * 5;
        for (XYChart.Series<Number, Number> s : series.values()) {
            while (s.getData().size() > maxSamples) {
                s.getData().remove(0);
            }
        }
    }

    public void toggleChannel(String name) {
        XYChart.Series<Number, Number> s = series.get(name);
        if (s == null) return;
        if (enabledChannels.contains(name)) {
            enabledChannels.remove(name);
            // Remove series from chart but keep data
            chart.getData().remove(s);
        } else {
            enabledChannels.add(name);
            // Add series back to chart
            if (!chart.getData().contains(s)) {
                chart.getData().add(s);
            }
        }
    }

    public boolean isPaused() { return paused; }
    public int getEnabledChannelCount() { return enabledChannels.size(); }
    public int getSampleCount() { return sampleCount; }
}
