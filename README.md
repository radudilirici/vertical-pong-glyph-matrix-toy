# Vertical Pong Glyph Matrix Toy

<img src="artwork/preview-2.svg" alt="Vertical Pong Glyph Matrix Toy" width="320">

Vertical Pong is a Pong game built for the Nothing Phone (3) Glyph Matrix. This version features a
vertical playstyle for better control and easier visibility at all times.
You control the lower paddle by tilting the phone left or right.

The first to score 11 points wins the game.
The toy includes three difficulty levels: Easy, Medium, and Hard.

This project was inspired by this [Pong Glyph Toy](https://playground.nothing.tech/detail/toy/bptqXndTIvQPXqPm)
from [Thomas](https://playground.nothing.tech/creator/ii0g5zW6ldBJoQKf).

## How to install

1. Install and open the app on a Nothing Phone (3).
2. Tap **Activate toy** to open the Glyph Toys manager.
3. Select **Vertical Pong** and activate it.
4. Increase the timeout duration so that your games are not suddenly stopped by the phone. (Optional, but highly recommended)

## How to play

1. Navigate to the new toy on the Glyph Matrix. The difficulty menu will appear.
2. Tilt left or right to select **EASY**, **MED**, or **HARD**.
3. Long press to start the match.
4. Tilt left and right to move the lower paddle.
5. Be the first player to score 11 points.

Long press during a match to return to the difficulty menu.

## Requirements

- Nothing Phone (3)
- A Nothing OS version that supports Glyph Matrix Toys
- Android 15 or newer

If matches are interrupted, increase the Glyph Toys timeout in the phone's
settings.

## Building

This project uses the Nothing Glyph Matrix SDK 2.0. The SDK AAR is not included
in this repository. Download it from the
[official Glyph Matrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)
and place it at:

```text
app/libs/GlyphMatrixSDK.aar
```

Then open the project in Android Studio or build the debug APK with:

```powershell
.\gradlew.bat :app:assembleDebug
```

Use of the Glyph Matrix SDK is governed by
[Nothing's SDK licence](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/blob/main/LICENSE.md).

## Contributing

I am open to any suggestions.
Tell me your thoughts and feel free to fork the project and create Pull Requests. 
