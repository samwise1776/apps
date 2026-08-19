using Raylib_cs;
using System;
using System.Numerics;

public static class Game3
{
    static Rectangle player, greenThing, redThing;
    static float redSpeed;
    static int points;
    static string message = "";
    static bool started;

    public static bool IsComplete { get; private set; }

    public static void Start()
    {
        started = true;
        IsComplete = false;
        points = 0;
        redSpeed = 170f;
        player = new Rectangle(100, 300, 40, 40);
        greenThing = new Rectangle(620, 280, 35, 35);
        redThing = new Rectangle(400, 250, 55, 55);
        message = "Collect 20 green things. Do NOT touch red.";
    }

    public static void Update(float dt, int w, int h)
    {
        if (!started) Start();

        Move(ref player, 270f, dt, w, h);
        redThing.X += redSpeed * dt;
        if (redThing.X <= 0 || redThing.X + redThing.Width >= w) redSpeed *= -1;

        if (Raylib.CheckCollisionRecs(player, greenThing))
        {
            points++;
            greenThing.X = Raylib.GetRandomValue(50, w - 80);
            greenThing.Y = Raylib.GetRandomValue(180, h - 70);

            message = points switch
            {
                1 => "You got one. Unfortunately.",
                5 => "5 green things collected.",
                10 => "10 points. Why?",
                15 => "STOP BEING GOOD AT THIS.",
                >= 20 => "GAME 3 COMPLETE. GAME 4 INCOMING.",
                _ => $"Green things: {points} / 20"
            };

            if (points >= 20) IsComplete = true;
        }

        if (!IsComplete && Raylib.CheckCollisionRecs(player, redThing))
        {
            points = 0;
            message = "YOU TOUCHED RED. SCORE DELETED.";
            player = new Rectangle(100, 300, 40, 40);
            redThing = new Rectangle(400, 250, 55, 55);
        }
    }

    public static void Draw(Font font, int w, int h)
    {
        Raylib.ClearBackground(new Color(10, 38, 38, 255));
        Center(font, "GAME 3", 20, 42, Color.White, w);
        Center(font, "THIS IS APPARENTLY A GAME NOW", 70, 22, new Color(130, 255, 190, 255), w);
        Center(font, message, 110, 18, Color.LightGray, w);
        Center(font, "WASD = Move", 140, 16, Color.Gray, w);

        Raylib.DrawRectangleRec(player, new Color(70, 150, 255, 255));
        Raylib.DrawRectangleRounded(greenThing, 0.3f, 10, new Color(80, 255, 120, 255));
        Raylib.DrawRectangleRounded(redThing, 0.4f, 10, new Color(255, 70, 70, 255));
        Raylib.DrawCircle((int)redThing.X + 17, (int)redThing.Y + 20, 4, Color.White);
        Raylib.DrawCircle((int)redThing.X + 38, (int)redThing.Y + 20, 4, Color.White);
        Raylib.DrawTextEx(font, $"Game 3 points: {points} / 20", new Vector2(20, h - 42), 22, 1, Color.White);
    }

    static void Move(ref Rectangle r, float speed, float dt, int w, int h)
    {
        if (Raylib.IsKeyDown(KeyboardKey.W)) r.Y -= speed * dt;
        if (Raylib.IsKeyDown(KeyboardKey.S)) r.Y += speed * dt;
        if (Raylib.IsKeyDown(KeyboardKey.A)) r.X -= speed * dt;
        if (Raylib.IsKeyDown(KeyboardKey.D)) r.X += speed * dt;
        r.X = Math.Clamp(r.X, 0, w - r.Width);
        r.Y = Math.Clamp(r.Y, 160, h - r.Height);
    }

    static void Center(Font f, string text, float y, float size, Color c, int w)
    {
        Vector2 m = Raylib.MeasureTextEx(f, text, size, 1);
        Raylib.DrawTextEx(f, text, new Vector2((w - m.X) / 2, y), size, 1, c);
    }
}