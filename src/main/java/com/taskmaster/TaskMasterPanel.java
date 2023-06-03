package com.taskmaster;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import okhttp3.*;
import net.runelite.client.ui.components.ProgressBar;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;


@Slf4j
public class TaskMasterPanel extends PluginPanel {

    private final Client client;

    public final JPanel infoPanel = new JPanel();
    public final JPanel currentTaskPanel = new JPanel();
    private final TaskMasterPlugin plugin;
    final JLabel currentTaskLabel = new JLabel();
    final JLabel currentTaskHead = new JLabel();
    private final ProgressBar progressBar = new ProgressBar();




    @SneakyThrows
    TaskMasterPanel(TaskMasterPlugin plugin, TaskMasterConfig config, Client client, OkHttpClient okHttpClient) {
       this.client = client;
       this.plugin=plugin;
       setBorder(new EmptyBorder(6, 6, 6, 6));
       setBackground(ColorScheme.DARK_GRAY_COLOR);
       setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));




        final JPanel layoutPanel = new JPanel();
        layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.Y_AXIS));
        add(layoutPanel);
        infoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        infoPanel.setLayout(new GridBagLayout());
        infoPanel.setVisible(true);

        final JButton getInfoButton = new JButton("Get Task Info");
        getInfoButton.addActionListener( e ->
        {
            try {
                plugin.getTask();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } );
        final JButton updateButton = new JButton("Send Update");

        updateButton.setSize(getInfoButton.getSize());

        infoPanel.add(getInfoButton);
        infoPanel.add(updateButton);

        currentTaskPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        currentTaskPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        currentTaskPanel.setLayout(new GridLayout(3,0));
        currentTaskPanel.setVisible(true);

        currentTaskHead.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentTaskHead.setForeground(Color.WHITE);
        currentTaskHead.setBackground(Color.DARK_GRAY);
        currentTaskHead.setText("Current Task: ");

        currentTaskPanel.add(currentTaskHead);

        currentTaskLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentTaskLabel.setForeground(Color.WHITE);
        currentTaskLabel.setBackground(Color.DARK_GRAY);
        currentTaskLabel.setText("Not Loaded");

        currentTaskPanel.add(currentTaskLabel);
        currentTaskPanel.add(progressBar);




        layoutPanel.add(infoPanel);
        layoutPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        layoutPanel.add(currentTaskPanel);

    }

    public void updateCurrent(int current)
    {
        progressBar.setCenterLabel(String.valueOf(current));
        progressBar.setValue(current);
    }

    public void processTask(CurrentTask task)
    {
        currentTaskLabel.setText(task.getTask());
        progressBar.setMaximumValue(task.getGoal());
        progressBar.setLeftLabel(task.getStart() + "");
        progressBar.setCenterLabel(task.getCurrent() + "");
        progressBar.setRightLabel(task.getGoal() + "");
        progressBar.setValue(task.getCurrent());
    }
}