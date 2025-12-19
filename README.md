# 🐸 DiffFrog
<p align="center">
  <img
    src="https://github.com/user-attachments/assets/28d97875-b088-4ed0-89d0-460d80211e5d"
    style="width:49%; "
    alt="difffrog gif"
  />
  <img
    src="https://github.com/user-attachments/assets/22f0fec2-97ee-45a9-993d-b481dc5cb6e6"
    style="width:10%;"
    alt="difffrog img"
  />
</p>



*DiffFrog* is a lightweight, kawaii-styled IntelliJ IDEA extension that helps you monitor your Git changes in real-time directly from your status bar. Keep your Pull Requests under control with a touch of personality!

---

## ✨ Key Features

* *📊 Live Stats:* Instant counter of added (+) and deleted (-) lines compared to your target branch (develop, main, etc.).
* *🎢 Counter Animation:* Smooth "slot-machine" style numbering that updates every time you save your work.
* *🌡️ Dynamic Heatmap:* The color of added lines scales from neutral white to intense green as you approach your line limit.
* *🌿 The 420 Milestone:* * *Warning:* Reaching your line limit triggers a ⚠️ alert symbol.
    * *Perfect Balance:* Achieving exactly *420 additions and 420 deletions* unlocks the special 🌿 icon.
* *⚙️ Fast Config:* One-click access to change target branches, line limits, and refresh intervals.

---

## 🚀 Installation

1. Open *IntelliJ IDEA*.
2. Navigate to Settings > Plugins > Marketplace.
3. Search for *"DiffFrog"*.
4. Click *Install* and restart your IDE.

---

## 🛠️ Configuration

Click on the stats label in the toolbar to open the settings popup:

| Setting | Description |
| :--- | :--- |
| *Branch* | The branch to compare your current work against (e.g., master). |
| *Max Lines* | The threshold for the green heatmap and alerts (Default: 420). |
| *Refresh Speed* | How often the frog scans for changes (from 🐢 Slow to ⚡ Instant). |

---

## 🏗️ Technical Stack

* *Language:* Kotlin
* *Framework:* IntelliJ Platform SDK
* *VCS Integration:* Git4Idea (Native Git support)

---

## 🤝 Contributing

Want to make the frog even better?
1. Fork the project.
2. Create your feature branch (git checkout -b feature/FrogImprovement).
3. Commit your changes (git commit -m 'Add some froggy magic').
4. Push to the branch (git push origin feature/FrogImprovement).
5. Open a Pull Request.

---
