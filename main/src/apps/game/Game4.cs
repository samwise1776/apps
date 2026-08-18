using Raylib_cs;
using System;
using System.Numerics;

public static class Game4
{
    static Rectangle player, target, enemy;
    static float enemySpeed;
    static int score;
    static string message = "";
    static bool started;

    public static void Start()
    {
        started = true;
        score = 0;
        enemySpeed = 190f;
        player = new Rectangle(100, 300, 40, 40);
        target = new Rectangle(620, 300, 35, 35);
        enemy = new Rectangle(380, 220, 55, 55);
        message = "GAME 4 STARTED. Collect the green square.";
    }

    public static void Update(float dt, int w, int h)
    {
        if (!started) Start();

        Move(ref player, 280f, dt, w, h);
        enemy.X += enemySpeed * dt;
        if (enemy.X <= 0 || enemy.X + enemy.Width >= w) enemySpeed *= -1;

        if (Raylib.CheckCollisionRecs(player, target))
        {
            score++;
            target.X = Raylib.GetRandomValue(50, w - 80);
            target.Y = Raylib.GetRandomValue(180, h - 70);

            message = score switch
            {
                1 => "You found a green square.",
                3 => "Why are you collecting these?",
                5 => "5 POINTS. GAME 4 IS CONFUSED.",
                10 => "10 POINTS. Something is happening.",
                15 => "STOP COLLECTING THEM.",
                >= 20 => "YOU BEAT GAME 4.",
                _ => $"Score: {score}"
            };
        }

        if (Raylib.CheckCollisionRecs(player, enemy))
        {
            score = 0;
            player = new Rectangle(100, 300, 40, 40);
            enemy = new Rectangle(380, 220, 55, 55);
            message = "You touched the red thing. Score deleted.";
        }
    }

    public static void Draw(Font font, int w, int h)
    {
        Raylib.ClearBackground(new Color(20, 13, 35, 255));

        for (int i = 0; i < 10; i++)
        {
            float x = 40 + i * 85;
            float y = 250 + MathF.Sin((float)Raylib.GetTime() * 2 + i) * 45;
            Raylib.DrawCircle((int)x, (int)y, 8, new Color(75, 40, 105, 255));
        }

        Center(font, "GAME 4", 20, 42, Color.White, w);
        Center(font, "THE FOURTH THING", 68, 21, new Color(200, 140, 255, 255), w);
        Center(font, message, 105, 18, Color.LightGray, w);
        Center(font, "WASD = Move", 135, 16, Color.Gray, w);

        Raylib.DrawRectangleRounded(player, 0.2f, 8, new Color(70, 150, 255, 255));
        Raylib.DrawRectangleRounded(target, 0.4f, 8, new Color(70, 255, 120, 255));
        Raylib.DrawRectangleRounded(enemy, 0.35f, 10, new Color(255, 65, 80, 255));
        Raylib.DrawCircle((int)enemy.X + 17, (int)enemy.Y + 19, 4, Color.White);
        Raylib.DrawCircle((int)enemy.X + 38, (int)enemy.Y + 19, 4, Color.White);
        Raylib.DrawTextEx(font, $"Game 4 Score: {score}", new Vector2(20, h - 42), 22, 1, Color.White);
    }

    static void Move(ref Rectangle r, float speed, float dt, int w, int h)
    {
        if (Raylib.IsKeyDown(KeyboardKey.W)) r.Y -= speed * dt;
        if (Raylib.IsKeyDown(KeyboardKey.S)) r.Y += speed * dt;
        if (Raylib.IsKeyDown(KeyboardKey.A)) r.X -= speed * dt;
        if (Raylib.IsKeyDown(KeyboardKey.D)) r.X += speed * dt;
        r.X = Math.Clamp(r.X, 0, w - r.Width);
        r.Y = Math.Clamp(r.Y, 150, h - r.Height);
    }

    static void Center(Font f, string text, float y, float size, Color c, int w)
    {
        Vector2 m = Raylib.MeasureTextEx(f, text, size, 1);
        Raylib.DrawTextEx(f, text, new Vector2((w - m.X) / 2, y), size, 1, c);
    }
}
