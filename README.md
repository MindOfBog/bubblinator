# Bubblinator
A tool for the mainline LBP games, that can generate a PLAN file containing prize bubbles from a list of items.

### Requirements
- JRE 11 or up
- [Toolkit](https://github.com/ennuo/toolkit)

### Usage
- Load a list of items either via the top button from a PAL, MOD or MAP file, or manually add each descriptor from the "Add plan descriptor" button.
- `Bubblebomb` places each prize bubble in the same spot, so instead of columns you just have one bubble. Use with caution.
- `Prefer hash` uses the SHA1 of each item instead of GUID if both are present, when unchecked, it defaults to using GUID.
- `Shareable` makes each item shareable in game.
- `Max bubbles per column` sets the maximum amount of bubbles in each column.
- After you have your list and have selected your settings, you can either build the bubbles as a PLAN (object for your popit) or BIN (level to put on your moon).
- Once you have a PLAN or BIN, add them to your mod, profile or game data via toolkit, with those loaded `Archive>Add...` and select your PLAN or BIN.

### Troubleshooting
- If you get errors opening the tool, likely culprit is your Java Runtime Environment. Use JRE 11 or up.
- If you can't select files when clicking any of the buttons, select `Legacy File Dialogue` as your OS's file picker is likely not supported by TinyFD.

### Thanks
Thanks [ennuo](https://github.com/ennuo) for creating [Toolkit/CWLib](https://github.com/ennuo/toolkit) it is used by this tool to read and create files.

<img src="https://github.com/MindOfBog/bubblinator/blob/main/images/tool.png?raw=true" height=256 /> <img src="https://github.com/MindOfBog/bubblinator/blob/main/images/item.png?raw=true" height=256 /> <img src="https://github.com/MindOfBog/bubblinator/blob/main/images/bubbles.png?raw=true" height=256 />
