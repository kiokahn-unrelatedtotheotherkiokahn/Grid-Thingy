package com.grid;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main implements ApplicationListener {
    private ShapeRenderer sr;
    private int[][] grid =
        {
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
        };
    int currentRow;
    int currentColumn;
    int goldRow;
    int goldColumn;
    Sound collectSound;
    Sound gameOverSound;
    private BitmapFont font;
    private SpriteBatch spriteBatch;
    private float time = 5F;
    private float currentTimeGain = 2F;
    int score = 0;
    boolean soundPlayed;
    Integer[] highscores;
    int endScore;


    @Override
    public void create() {
        for (int i = 1; i <= 3; i++) {
            getScoreFromFirebase(i);
        }
        sr = new ShapeRenderer();
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(2F);

        goldColumn = MathUtils.random(0, grid[0].length - 1);
        goldRow = MathUtils.random(0, grid.length - 1);
        grid[goldRow][goldColumn] = 2;

        int randomColumn2 = MathUtils.random(0, grid[0].length - 1);
        int randomRow2 = MathUtils.random(0, grid.length - 1);
        while (randomColumn2 == goldColumn && goldRow == randomRow2) {
            randomColumn2 = MathUtils.random(0, grid[0].length - 1);
            randomRow2 = MathUtils.random(0, grid.length - 1);
        }

        grid[randomRow2][randomColumn2] = 1;
        currentRow = randomRow2;
        currentColumn = randomColumn2;

        collectSound = Gdx.audio.newSound(Gdx.files.internal("cheer.mp3"));
        gameOverSound = Gdx.audio.newSound(Gdx.files.internal("crowdaw.mp3"));
        soundPlayed = false;
        highscores = new Integer[3];

    }

    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
        if (width <= 0 || height <= 0) return;

        // Resize your application here. The parameters represent the new window size.
    }

    @Override
    public void render() {
        if (time >= 0) {

            int tempRow = currentRow;
            int tempColumn = currentColumn;
            time -= Gdx.graphics.getDeltaTime();
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                tempRow--;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                tempRow++;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                tempColumn++;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                tempColumn--;
            }

            if (!(tempRow == currentRow && tempColumn == currentColumn)) {
                if (tempRow >= 0 && tempRow < grid.length) {
                    grid[currentRow][currentColumn] = 0;
                    grid[tempRow][currentColumn] = 1;
                    currentRow = tempRow;
                }
                if (tempColumn >= 0 && tempColumn < grid[0].length) {
                    grid[currentRow][currentColumn] = 0;
                    grid[currentRow][tempColumn] = 1;
                    currentColumn = tempColumn;
                }
            }

            Gdx.gl.glClearColor(0.15F, 0.15F, 0.2F, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // clear screen

            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    if (grid[i][j] == 1) {
                        sr.setColor(Color.FIREBRICK);
                    } else if (grid[i][j] == 2) {
                        sr.setColor(Color.GOLD);
                    } else {
                        sr.setColor(Color.GRAY);
                    }
                    sr.rect(100 + j * 80, 50 + (4 - i) * 80, 76, 76);
                }
            }
            if (currentRow == goldRow && currentColumn == goldColumn) {
                collectSound.play();
                score += 1;
                time += currentTimeGain;
                currentTimeGain = (float) (currentTimeGain * 0.9);
                goldColumn = MathUtils.random(0, grid[0].length - 1);
                goldRow = MathUtils.random(0, grid.length - 1);
                grid[goldRow][goldColumn] = 2;
            }

            sr.end();

            spriteBatch.begin();
            font.setColor(Color.WHITE);
            font.draw(spriteBatch, "Score: " + score, 10, Gdx.graphics.getHeight() - 10);
            font.draw(spriteBatch, "Time: " + (int) time, 10, Gdx.graphics.getHeight() - 60);
            spriteBatch.end();

        } else {
            Gdx.gl.glClearColor(0.15F, 0.2F, 0.15F, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            for (int i = 1; i <= 3; i++) {
                System.out.println("Score" + i + " = " + highscores[i - 1]);
            }
            spriteBatch.begin();
            font.draw(spriteBatch, "GAME OVER", ((float) Gdx.graphics.getWidth() / 2) - 80, ((float) Gdx.graphics.getHeight() / 2) + 50);
            font.draw(spriteBatch, "Press enter to play again", ((float) Gdx.graphics.getWidth() / 2) - 150, ((float) Gdx.graphics.getHeight() / 2) + 20);
            if (!soundPlayed) {
                gameOverSound.play();
                soundPlayed = true;
                if (score > highscores[0]) {
                    highscores[2] = highscores[1];
                    highscores[1] = highscores[0];
                    highscores[0] = score;
                } else if (score > highscores[1]) {
                    highscores[2] = highscores[1];
                    highscores[1] = score;
                } else if (score > highscores[2]) {
                    highscores[2] = score;
                }
                for (int i = 0; i <=2; i++){
                    updateScoreInFirebase(highscores[i], i+1);
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    time = 15F;
                    currentTimeGain = 2F;
                    score = 0;
                    soundPlayed = false;
            }
            spriteBatch.end();
        }
    }
        @Override
        public void pause () {
            // Invoked when your application is paused.
        }

        @Override
        public void resume () {
            // Invoked when your application is resumed after pause.
        }

        @Override
        public void dispose () {
            // Destroy application's resources here.
        }

        public void getScoreFromFirebase (int scoreNumber){
            String projectId = "gridamajig";
            String apiKey = "AIzaSyAkVE2EHTCshp7QTEOEy0DbnyTkbkcC_Z0";
            String collection = "firstgame";
            String documentId = "score";


            String url = "https://firestore.googleapis.com/v1/projects/" + projectId +
                "/databases/(default)/documents/" + collection + "/" + documentId + "?key=" + apiKey;


            Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
            request.setUrl(url);

            Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    HttpStatus status = httpResponse.getStatus();
                    if (status.getStatusCode() == 200) {
                        String responseJson = httpResponse.getResultAsString();

                        JsonReader reader = new JsonReader();
                        JsonValue base = reader.parse(responseJson);

                        String scoreStr = base.get("fields").get("score" + scoreNumber).getString("integerValue");
                        int score = Integer.parseInt(scoreStr);
                        for (int i = 0; i <= 2; i++) {
                            highscores[i] = score;
                        }
                        Gdx.app.log("Firebase", "Score succesful: " + score);

                    } else {
                        Gdx.app.log("Firebase", "Error: " + status.getStatusCode());
                    }
                }

                @Override
                public void failed(Throwable t) {
                    Gdx.app.log("Firebase", "Connection Error", t);
                }

                @Override
                public void cancelled() {
                    Gdx.app.log("Firebase", "Canceled Request");
                }
            });
        }


        public void updateScoreInFirebase (int newScore, int scoreNumber){
            String projectId = "gridamajig";
            String apiKey = "AIzaSyAkVE2EHTCshp7QTEOEy0DbnyTkbkcC_Z0";
            String collection = "firstgame";
            String documentId = "score";

            String url = "https://firestore.googleapis.com/v1/projects/" + projectId +
                "/databases/(default)/documents/" + collection + "/" + documentId +
                "?key=" + apiKey + "&updateMask.fieldPaths=score"+scoreNumber;

            Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
            request.setUrl(url);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("X-HTTP-Method-Override", "PATCH");
            String jsonBody = "{" +
                "  \"fields\": {" +
                "    \"score"+scoreNumber+"\": { \"integerValue\": \"" + newScore + "\" }" +
                "  }" +
                "}";

            request.setContent(jsonBody);

            Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    HttpStatus status = httpResponse.getStatus();
                    if (status.getStatusCode() == 200) {
                        Gdx.app.log("Firebase", "Score updated to: " + newScore + "!");
                    } else {
                        Gdx.app.log("Firebase", "Update Error: " + status.getStatusCode() + " - " + httpResponse.getResultAsString());
                    }
                }

                @Override
                public void failed(Throwable t) {
                    Gdx.app.log("Firebase", "Connection Error", t);
                }

                @Override
                public void cancelled() {
                    Gdx.app.log("Firebase", "Canceled Request");
                }
            });
        }
    }

