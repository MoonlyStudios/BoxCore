package net.moonly.modules.scoreboard;

import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

public class ScoreboardTickable {
  private final List<String> lines;
  
  private final int updateTicks;
  
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
  
  public void setCurrentTick(int currentTick) {
    this.currentTick = currentTick;
  }
  
  public void setCurrentLine(String currentLine) {
    this.currentLine = currentLine;
  }
  
  public void setCurrentLineIndex(int currentLineIndex) {
    this.currentLineIndex = currentLineIndex;
  }
  
  public List<String> getLines() {
    return this.lines;
  }
  
  public int getUpdateTicks() {
    return this.updateTicks;
  }
  
  private boolean enabled = true;
  
  private int currentTick;
  
  private String currentLine;
  
  private int currentLineIndex;
  
  public boolean isEnabled() {
    return this.enabled;
  }
  
  public int getCurrentTick() {
    return this.currentTick;
  }
  
  public String getCurrentLine() {
    return this.currentLine;
  }
  
  public int getCurrentLineIndex() {
    return this.currentLineIndex;
  }
  
  public ScoreboardTickable(List<String> lines, int updateTicks) {
    this.lines = lines;
    this.updateTicks = updateTicks;
  }
  
  public ScoreboardTickable(ConfigurationSection configurationSection) {
    this.lines = configurationSection.getStringList("frames");
    this.updateTicks = configurationSection.getInt("ticks");
  }
  
  public String tick(boolean returnLine) {
    this.currentTick++;
    if (this.currentLine == null)
      this.currentLine = this.lines.get(this.currentLineIndex); 
    boolean sequenceOver = (this.currentTick == this.updateTicks);
    if (sequenceOver) {
      this.currentTick = 0;
      this.currentLineIndex++;
      if (this.currentLineIndex >= this.lines.size())
        this.currentLineIndex = 0; 
      this.currentLine = this.lines.get(this.currentLineIndex);
      if (!returnLine)
        this.currentTick--; 
    } 
    return sequenceOver ? (
      returnLine ? this.currentLine : null) : 
      this.currentLine;
  }
  
  public void reset() {
    this.currentTick = 0;
    this.currentLineIndex = 0;
    this.currentLine = this.lines.get(0);
  }
}
