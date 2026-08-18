using Raylib_cs;
using System;
using System.IO;
using System.Numerics;

class Program
{
    enum GameState { Intro, MainGame, NonsenseChamber, Game3, Game4 }

    static void Main()
    {
        const int W = 800, H = 480;
        Raylib.InitWindow(W, H, "A Game That Makes No Sense");
        Raylib.SetTargetFPS(60);

        Font font = LoadBestFont(out bool customFont);
        GameState state = GameState.Intro;

        int clicks = 0, score = 0, chamberTouches = 0;
        string introMessage = "Press the button 100 times to start the game.";
        string gameMessage = "Use WASD to collect the yellow square.";
        string chamberMessage = "WELCOME TO THE NONSENSE CHAMBER.";

        Rectangle button = new(260, 240, 280, 70);
        Rectangle player = new(100, 300, 40, 40);
        Rectangle target = new(600, 300, 35, 35);
        Rectangle mysteryBox = new(570, 300, 80, 80);
        float chamberTimer = 0f;

        while (!Raylib.WindowShouldClose())
        {
            float dt = Raylib.GetFrameTime();
            Vector2 mouse = Raylib.GetMousePosition();

            if (state == GameState.Intro)
            {
                bool hover = Raylib.CheckCollisionPointRec(mouse, button);
                if (hover && Raylib.IsMouseButtonPressed(MouseButton.Left))
                {
                    clicks++;
                    introMessage = IntroMessage(clicks);
                    if (clicks >= 100)
                    {
                        state = GameState.MainGame;
                        player = new Rectangle(100, 300, 40, 40);
                        gameMessage = "THE GAME STARTED! Collect 10 yellow squares.";
                    }
                }
            }
            else if (state == GameState.MainGame)
            {
                Move(ref player, 250f, dt, W, H);
                if (Raylib.CheckCollisionRecs(player, target))
                {
                    score++;
                    target.X = Raylib.GetRandomValue(40, W - 80);
                    target.Y = Raylib.GetRandomValue(170, H - 80);
                    gameMessage = score switch
                    {
                        1 => "You collected a square. Amazing.",
                        5 => "5 POINTS. Something feels wrong.",
                        8 => "8 POINTS. Where are you going?",
                        9 => "9 POINTS. DO NOT GET ANOTHER ONE.",
                        _ => $"Score: {score} / 10"
                    };

                    if (score >= 10)
                    {
                        state = GameState.NonsenseChamber;
                        player = new Rectangle(100, 300, 40, 40);
                        chamberTouches = 0;
                        chamberTimer = 0f;
                        chamberMessage = "WELCOME TO THE NONSENSE CHAMBER.";
                    }
                }
            }
            else if (state == GameState.NonsenseChamber)
            {
                chamberTimer += dt;
                Move(ref player, 250f, dt, W, H);

                if (Raylib.CheckCollisionRecs(player, mysteryBox))
                {
                    chamberTouches++;
                    if (chamberTouches >= 20)
                    {
                        state = GameState.Game3;
                        Game3.Start();
                    }
                    else
                    {
                        mysteryBox.X = Raylib.GetRandomValue(100, 650);
                        mysteryBox.Y = Raylib.GetRandomValue(180, 380);
                        chamberMessage = chamberTouches switch
                        {
                            1 => "Why did you touch that?",
                            2 => "You touched it AGAIN.",
                            3 => "The chamber noticed you.",
                            5 => "5 touches. Absolutely incredible.",
                            10 => "10 TOUCHES. HALF WAY.",
                            15 => "15 TOUCHES. SOMETHING IS COMING.",
                            19 => "DO NOT TOUCH IT AGAIN.",
                            _ => $"Mystery box touches: {chamberTouches}"
                        };
                    }
                }
            }
            else if (state == GameState.Game3)
            {
                Game3.Update(dt, W, H);
                if (Game3.IsComplete)
                {
                    state = GameState.Game4;
                    Game4.Start();
                }
            }
            else if (state == GameState.Game4)
            {
                Game4.Update(dt, W, H);
            }

            Raylib.BeginDrawing();

            if (state == GameState.Intro)
            {
                Raylib.ClearBackground(new Color(24, 26, 36, 255));
                Center(font, "A GAME THAT MAKES NO SENSE", 70, 42, Color.White, W);
                Center(font, introMessage, 150, 22, Color.LightGray, W);

                bool hover = Raylib.CheckCollisionPointRec(mouse, button);
                Raylib.DrawRectangleRounded(button, 0.25f, 12,
                    hover ? new Color(145, 110, 255, 255) : new Color(115, 85, 230, 255));
                CenterInRect(font, "PRESS FOR NO REASON", button, 24, Color.White);
                Center(font, $"Times you ignored the warning: {clicks} / 100", 350, 20, Color.Gray, W);
            }
            else if (state == GameState.MainGame)
            {
                Raylib.ClearBackground(new Color(22, 26, 38, 255));
                Center(font, "THE GAME HAS STARTED", 25, 34, Color.White, W);
                Center(font, gameMessage, 78, 18, Color.LightGray, W);
                Center(font, "WASD = Move", 110, 18, Color.Gray, W);
                Raylib.DrawRectangleRec(player, new Color(70, 150, 255, 255));
                Raylib.DrawRectangleRec(target, new Color(255, 220, 70, 255));
                Raylib.DrawTextEx(font, $"Score: {score} / 10", new Vector2(20, 435), 24, 1, Color.White);
            }
            else if (state == GameState.NonsenseChamber)
            {
                Raylib.ClearBackground(new Color(38, 16, 55, 255));
                for (int i = 0; i < 8; i++)
                {
                    float x = 50 + i * 100;
                    float y = 220 + MathF.Sin(chamberTimer * 2 + i) * 50;
                    Raylib.DrawCircle((int)x, (int)y, 15, new Color(90, 45, 120, 255));
                }
                Center(font, "THE NONSENSE CHAMBER", 20, 38, new Color(255, 120, 255, 255), W);
                Center(font, chamberMessage, 75, 20, Color.White, W);
                Center(font, "Get 20 touches.", 115, 18, Color.LightGray, W);
                Raylib.DrawRectangleRec(player, new Color(70, 150, 255, 255));
                Raylib.DrawRectangleRounded(mysteryBox, 0.25f, 10, new Color(255, 80, 170, 255));
                Raylib.DrawTextEx(font, "?", new Vector2(mysteryBox.X + 27, mysteryBox.Y + 13), 45, 1, Color.White);
                Raylib.DrawTextEx(font, $"Chamber touches: {chamberTouches} / 20", new Vector2(20, 435), 20, 1, Color.White);
            }
            else if (state == GameState.Game3)
                Game3.Draw(font, W, H);
            else if (state == GameState.Game4)
                Game4.Draw(font, W, H);

            Raylib.EndDrawing();
        }

        if (customFont) Raylib.UnloadFont(font);
        Raylib.CloseWindow();
    }

    static Font LoadBestFont(out bool custom)
    {
        string[] paths =
        {
            "resources/GameFont.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"
        };

        foreach (string p in paths)
        {
            if (File.Exists(p))
            {
                custom = true;
                return Raylib.LoadFontEx(p, 64, null, 0);
            }
        }

        custom = false;
        return Raylib.GetFontDefault();
    }

    static string IntroMessage(int clicks) => clicks switch
    {
        1 => "Why did you press that?",
        2 => "You did it AGAIN.",
        3 => "Please stop.",
        4 => "The button is becoming concerned.",
        5 => "Achievement unlocked: BUTTON.",
        10 => "10 clicks. You accomplished nothing.",
        20 => "WHY ARE YOU STILL CLICKING?",
        30 => "Okay. You win. Probably.",
        50 => "YOU ARE ONLY HALFWAY THERE.",
        75 => "25 more. There might actually be a game.",
        90 => "Wait... something is happening.",
        95 => "5 MORE CLICKS.",
        99 => "ONE MORE.",
        >= 100 => "THE GAME IS STARTING.",
        >= 50 => $"STOP CLICKING. {clicks}/100",
        _ => $"Clicks: {clicks} / 100"
    };

    static void Move(ref Rectangle r, float speed, float dt, int w, int h)
    {
        if (Raylib.IsKeyDown(KeyboardKey.W)) r.Y -= speed * dt;
        if (Raylib.IsKeyDown(KeyboardKey.S)) r.Y += speed * dt;
        if (Raylib.IsKeyDown(KeyboardKey.A)) r.X -= speed * dt;
        if (Raylib.IsKeyDown(KeyboardKey.D)) r.X += speed * dt;
        r.X = Math.Clamp(r.X, 0, w - r.Width);
        r.Y = Math.Clamp(r.Y, 160, h - r.Height);
    }

    static void Center(Font font, string text, float y, float size, Color color, int width)
    {
        Vector2 m = Raylib.MeasureTextEx(font, text, size, 1);
        Raylib.DrawTextEx(font, text, new Vector2((width - m.X) / 2, y), size, 1, color);
    }

    static void CenterInRect(Font font, string text, Rectangle r, float size, Color color)
    {
        Vector2 m = Raylib.MeasureTextEx(font, text, size, 1);
        Raylib.DrawTextEx(font, text,
            new Vector2(r.X + (r.Width - m.X) / 2, r.Y + (r.Height - m.Y) / 2),
            size, 1, color);
    }
}