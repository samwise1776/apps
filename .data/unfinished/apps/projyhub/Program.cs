using System;
using Raylib_cs;

namespace RaylibExample;

// Define the different states our game can be in
enum GameState
{
    Title,
    Playing,
    GameOver
}

class Program
{
    static void Main(string[] args)
    {
        int screenWidth = 800;
        int screenHeight = 450;
        Raylib.InitWindow(screenWidth, screenHeight, "Chase Game - Raylib-cs");
        Raylib.SetTargetFPS(60);

        // Game State
        GameState currentState = GameState.Title;
        int score = 0;

        // Player setup
        float playerX = 100f;
        float playerY = 100f;
        float playerSpeed = 300f;
        int playerSize = 40;

        // Enemy setup
        float enemyX = 600f;
        float enemyY = 300f;
        float baseEnemySpeed = 150f;
        float enemySpeed = baseEnemySpeed;
        int enemySize = 40;

        // Coin setup
        float coinX = 400f;
        float coinY = 225f;
        int coinRadius = 15;

        Random random = new Random();

        // Main Game Loop
        while (!Raylib.WindowShouldClose())
        {
            float deltaTime = Raylib.GetFrameTime();

            // --- UPDATE (Game Logic) ---
            if (currentState == GameState.Title)
            {
                // Press Space to start
                if (Raylib.IsKeyPressed(KeyboardKey.Space))
                {
                    currentState = GameState.Playing;
                }
            }
            else if (currentState == GameState.Playing)
            {
                // 1. Player Movement (WASD)
                if (Raylib.IsKeyDown(KeyboardKey.W)) playerY -= playerSpeed * deltaTime;
                if (Raylib.IsKeyDown(KeyboardKey.S)) playerY += playerSpeed * deltaTime;
                if (Raylib.IsKeyDown(KeyboardKey.A)) playerX -= playerSpeed * deltaTime;
                if (Raylib.IsKeyDown(KeyboardKey.D)) playerX += playerSpeed * deltaTime;

                // 2. Keep player inside the screen bounds
                if (playerX < 0) playerX = 0;
                if (playerX > screenWidth - playerSize) playerX = screenWidth - playerSize;
                if (playerY < 0) playerY = 0;
                if (playerY > screenHeight - playerSize) playerY = screenHeight - playerSize;

                // 3. Enemy AI (Chase the player)
                float dx = playerX - enemyX;
                float dy = playerY - enemyY;
                float length = (float)Math.Sqrt(dx * dx + dy * dy);
                
                if (length > 0)
                {
                    // Normalize the direction and multiply by speed
                    enemyX += (dx / length) * enemySpeed * deltaTime;
                    enemyY += (dy / length) * enemySpeed * deltaTime;
                }

                // 4. Collision Logic
                Rectangle playerRec = new Rectangle(playerX, playerY, playerSize, playerSize);
                Rectangle enemyRec = new Rectangle(enemyX, enemyY, enemySize, enemySize);
                
                // Player vs Coin
                if (Raylib.CheckCollisionCircleRec(new System.Numerics.Vector2(coinX, coinY), coinRadius, playerRec))
                {
                    score++;
                    enemySpeed += 10f; // Make the game harder!
                    
                    // Move coin to a new random location
                    coinX = random.Next(coinRadius, screenWidth - coinRadius);
                    coinY = random.Next(coinRadius, screenHeight - coinRadius);
                }

                // Player vs Enemy
                if (Raylib.CheckCollisionRecs(playerRec, enemyRec))
                {
                    currentState = GameState.GameOver;
                }
            }
            else if (currentState == GameState.GameOver)
            {
                // Press Space to restart
                if (Raylib.IsKeyPressed(KeyboardKey.Space))
                {
                    // Reset variables
                    score = 0;
                    playerX = 100f;
                    playerY = 100f;
                    enemyX = 600f;
                    enemyY = 300f;
                    enemySpeed = baseEnemySpeed;
                    currentState = GameState.Playing;
                }
            }

            // --- DRAW (Rendering) ---
            Raylib.BeginDrawing();
            Raylib.ClearBackground(Color.RayWhite);

            if (currentState == GameState.Title)
            {
                Raylib.DrawText("CHASE GAME", 280, 150, 40, Color.DarkGray);
                Raylib.DrawText("Press SPACE to Start", 290, 220, 20, Color.Gray);
            }
            else if (currentState == GameState.Playing)
            {
                // Draw Coin
                Raylib.DrawCircle((int)coinX, (int)coinY, coinRadius, Color.Gold);

                // Draw Enemy
                Raylib.DrawRectangle((int)enemyX, (int)enemyY, enemySize, enemySize, Color.Purple);

                // Draw Player
                Raylib.DrawRectangle((int)playerX, (int)playerY, playerSize, playerSize, Color.Red);

                // Draw UI
                Raylib.DrawText($"Score: {score}", 10, 10, 20, Color.DarkGray);
            }
            else if (currentState == GameState.GameOver)
            {
                Raylib.DrawText("GAME OVER!", 290, 150, 40, Color.Red);
                Raylib.DrawText($"Final Score: {score}", 330, 200, 20, Color.DarkGray);
                Raylib.DrawText("Press SPACE to Restart", 280, 250, 20, Color.Gray);
            }

            Raylib.EndDrawing();
        }

        Raylib.CloseWindow();
    }
}