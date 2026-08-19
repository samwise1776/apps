import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class App extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JTextPane chatArea;
    private final JTextField inputField;
    private final JButton sendButton;
    private final StringBuilder conversationHtml = new StringBuilder();

    private final Brain brain;
    private final OfflineAssistant assistant;

    public App() {

        /*
         * Create and train the AI.
         */
        brain = new Brain();
        assistant = new OfflineAssistant(brain);

        setTitle("Nova — Local Java AI");
        setSize(980, 720);
        setMinimumSize(new Dimension(720, 520));

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        setLocationRelativeTo(null);


        /*
         * ================================
         * CHAT AREA
         * ================================
         */

        chatArea = new JTextPane();

        chatArea.setEditable(false);
        chatArea.setContentType("text/html");
        chatArea.setBackground(new Color(15, 23, 42));

        chatArea.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        16
                )
        );

        JScrollPane scrollPane =
                new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);


        /*
         * ================================
         * INPUT
         * ================================
         */

        inputField =
                new JTextField();

        inputField.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        16
                )
        );
        inputField.setBackground(new Color(30, 41, 59));
        inputField.setForeground(new Color(241, 245, 249));
        inputField.setCaretColor(Color.WHITE);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));


        sendButton =
                new JButton("Send");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        sendButton.setForeground(Color.WHITE);
        sendButton.setBackground(new Color(79, 70, 229));
        sendButton.setFocusPainted(false);
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setMargin(new Insets(10, 22, 10, 22));


        JPanel bottomPanel =
                new JPanel(
                        new BorderLayout(
                                5,
                                5
                        )
                );
        bottomPanel.setBackground(new Color(15, 23, 42));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));


        bottomPanel.add(
                inputField,
                BorderLayout.CENTER
        );

        bottomPanel.add(
                sendButton,
                BorderLayout.EAST
        );


        /*
         * ================================
         * WINDOW
         * ================================
         */

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 41, 59));
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JLabel title = new JLabel("NOVA");
        title.setForeground(new Color(248, 250, 252));
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel subtitle = new JLabel("100% local • pure Java • offline");
        subtitle.setForeground(new Color(148, 163, 184));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        header.add(title, BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        add(
                bottomPanel,
                BorderLayout.SOUTH
        );


        /*
         * Send button
         */

        sendButton.addActionListener(
                e -> sendMessage()
        );


        /*
         * Pressing ENTER also sends.
         */

        inputField.addActionListener(
                e -> sendMessage()
        );


        addBubble("Nova", "Hello! Ask me general questions, calculations, definitions, programming questions, or everyday advice. I run entirely inside Java.", false);


        setVisible(true);
    }


    /*
     * =========================================
     * SEND MESSAGE
     * =========================================
     */

    private void sendMessage() {

        String message =
                inputField
                        .getText()
                        .trim();


        if (message.isEmpty()) {
            return;
        }


        addBubble("You", message, true);


        inputField.setText("");


        /*
         * The ACTUAL text message now
         * goes into the neural network.
         */

        Brain.Answer answer = assistant.answer(message);


        giveAnswerText(answer);


        /*
         * Scroll downward.
         */

        chatArea.setCaretPosition(
                chatArea
                        .getDocument()
                        .getLength()
        );
    }


    /*
     * =========================================
     * DISPLAY AI ANSWER
     * =========================================
     */

    private void giveAnswerText(
            Brain.Answer answer
    ) {

        addBubble("Nova", answer.text, false);
    }

    private void addBubble(String name, String text, boolean user) {
        String background = user ? "#4f46e5" : "#1e293b";
        String align = user ? "right" : "left";
        conversationHtml.append("<div style='text-align:").append(align)
                .append(";margin:12px 18px'><span style='color:#94a3b8;font-size:10px'>")
                .append(escapeHtml(name)).append("</span><br><span style='display:inline-block;background:")
                .append(background).append(";color:#f8fafc;padding:10px 14px'>")
                .append(escapeHtml(text).replace("\n", "<br>"))
                .append("</span></div>");
        chatArea.setText("<html><body style='background:#0f172a;font-family:sans-serif'>"
                + conversationHtml + "</body></html>");
        SwingUtilities.invokeLater(() -> chatArea.setCaretPosition(chatArea.getDocument().getLength()));
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }


    /*
     * =========================================
     * START PROGRAM
     * =========================================
     */

    public static void main(String[] args) {

        if (
                args.length > 1
                        &&
                "--ask".equals(args[0])
        ) {

            Brain brain = new Brain();
            OfflineAssistant assistant = new OfflineAssistant(brain);
            String question = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            System.out.println(assistant.answer(question).text);
            return;
        }

        if (
                args.length > 0
                        &&
                "--self-test".equals(args[0])
        ) {

            boolean passed =
                    Brain.runSelfTest();


            if (!passed) {

                System.exit(1);
            }


            return;
        }

        SwingUtilities.invokeLater(
                App::new
        );
    }
}


/*
 * ==================================================
 *
 *                     BRAIN
 *
 * ==================================================
 */

class Brain {

    /*
     * The dictionary converts words
     * into neural-network input positions.
     */

    private final AppDictionary dictionary =
            new AppDictionary();


    /*
     * Categories the AI can currently learn.
     */

    private final String[] categories = {

            "GREETING",
            "HOW_ARE_YOU",
            "THANKS",
            "GOODBYE",
            "HELP",
            "NAME",
            "YES",
            "NO",
            "CODING",
            "AI"

    };


    /*
     * Training sentences.
     *
     * Each array matches one category above.
     */

    private final String[][] trainingData = {

            /*
             * GREETING
             */
            {
                    "hello",
                    "hi",
                    "hey",
                    "hello there",
                    "hey there",
                    "good morning",
                    "good afternoon",
                    "good evening"
            },


            /*
             * HOW_ARE_YOU
             */
            {
                    "how are you",
                    "how are you doing",
                    "are you okay",
                    "how do you feel",
                    "are you doing well"
            },


            /*
             * THANKS
             */
            {
                    "thank you",
                    "thanks",
                    "thanks a lot",
                    "thank you very much",
                    "awesome thanks"
            },


            /*
             * GOODBYE
             */
            {
                    "bye",
                    "goodbye",
                    "see you",
                    "see you later",
                    "later",
                    "good night"
            },


            /*
             * HELP
             */
            {
                    "help",
                    "help me",
                    "can you help",
                    "can you help me",
                    "i need help",
                    "please help"
            },


            /*
             * NAME
             */
            {
                    "what is your name",
                    "who are you",
                    "tell me your name",
                    "what are you"
            },


            /*
             * YES
             */
            {
                    "yes",
                    "yeah",
                    "yep",
                    "sure",
                    "okay",
                    "ok"
            },


            /*
             * NO
             */
            {
                    "no",
                    "nope",
                    "nah",
                    "not really"
            },


            /*
             * CODING
             */
            {
                    "coding",
                    "programming",
                    "java",
                    "write code",
                    "help with code",
                    "software engineering",
                    "program",
                    "developer"
            },


            /*
             * AI
             */
            {
                    "artificial intelligence",
                    "ai",
                    "neural network",
                    "machine learning",
                    "how does ai work",
                    "teach me ai",
                    "neuron",
                    "training model"
            }
    };


    /*
     * weights[output neuron][input word]
     */

    private double[][] weights;


    /*
     * One bias per output neuron.
     */

    private double[] biases;


    /*
     * Learning rate.
     */

    private final double learningRate =
            0.03;


    /*
     * Random generator.
     */

    private final Random random =
            new Random(42);


    /*
     * =========================================
     * CONSTRUCTOR
     * =========================================
     */

    public Brain() {

        /*
         * First build the dictionary.
         */

        buildDictionary();


        /*
         * THEN create the network.
         *
         * This is important because the network
         * needs to know how many words exist.
         */

        createNetwork();


        /*
         * Train everything.
         */

        train(2500);


        System.out.println(
                "AI READY"
        );

        System.out.println(
                "Dictionary size: "
                        + dictionary.size()
        );
    }


    /*
     * =========================================
     * BUILD DICTIONARY
     * =========================================
     */

    private void buildDictionary() {

        for (
                String[] categoryExamples :
                trainingData
        ) {

            for (
                    String sentence :
                    categoryExamples
            ) {

                dictionary.learnSentence(
                        sentence
                );
            }
        }
    }


    /*
     * =========================================
     * CREATE NETWORK
     * =========================================
     */

    private void createNetwork() {

        int inputCount =
                dictionary.size();

        int outputCount =
                categories.length;


        weights =
                new double
                        [outputCount]
                        [inputCount];


        biases =
                new double
                        [outputCount];


        /*
         * Random starting weights.
         */

        for (
                int output = 0;
                output < outputCount;
                output++
        ) {

            for (
                    int input = 0;
                    input < inputCount;
                    input++
            ) {

                weights[output][input] =
                        (
                                random.nextDouble()
                                        - 0.5
                        )
                                * 0.1;
            }


            biases[output] =
                    0.0;
        }
    }


    /*
     * =========================================
     * FORWARD PASS
     * =========================================
     */

    private double[] calculateOutput(
            double[] inputs
    ) {

        double[] rawOutputs =
                new double[
                        categories.length
                        ];


        /*
         * Every category gets one output neuron.
         */

        for (
                int outputNeuron = 0;
                outputNeuron
                        < categories.length;
                outputNeuron++
        ) {

            /*
             * Start with bias.
             */

            double total =
                    biases[outputNeuron];


            /*
             * This is the expanded version of:
             *
             * x1*w1 + x2*w2 + bias
             *
             * except now there can be
             * dozens/hundreds of words.
             */

            for (
                    int inputNeuron = 0;
                    inputNeuron
                            < inputs.length;
                    inputNeuron++
            ) {

                total +=
                        inputs[inputNeuron]
                                *
                        weights
                                [outputNeuron]
                                [inputNeuron];
            }


            rawOutputs[outputNeuron] =
                    total;
        }


        /*
         * Convert the raw values
         * into probabilities.
         */

        return softmax(
                rawOutputs
        );
    }


    /*
     * =========================================
     * SOFTMAX
     * =========================================
     */

    private double[] softmax(
            double[] values
    ) {

        double maximum =
                Arrays
                        .stream(values)
                        .max()
                        .orElse(0.0);


        double[] probabilities =
                new double[
                        values.length
                        ];


        double total =
                0.0;


        /*
         * Exponential calculation.
         */

        for (
                int i = 0;
                i < values.length;
                i++
        ) {

            probabilities[i] =
                    Math.exp(
                            values[i]
                                    - maximum
                    );


            total +=
                    probabilities[i];
        }


        /*
         * Normalize everything
         * so the probabilities total 1.
         */

        for (
                int i = 0;
                i < probabilities.length;
                i++
        ) {

            probabilities[i] /=
                    total;
        }


        return probabilities;
    }


    /*
     * =========================================
     * TRAIN NETWORK
     * =========================================
     */

    private void train(
            int epochs
    ) {

        System.out.println(
                "Training AI..."
        );


        for (
                int epoch = 0;
                epoch < epochs;
                epoch++
        ) {

            double totalLoss =
                    0.0;


            int exampleCount =
                    0;


            /*
             * Go through every category.
             */

            for (
                    int correctCategory = 0;
                    correctCategory
                            < trainingData.length;
                    correctCategory++
            ) {


                /*
                 * Go through every sentence
                 * inside the category.
                 */

                for (
                        String sentence :
                        trainingData[
                                correctCategory
                                ]
                ) {

                    /*
                     * TEXT -> NUMBERS
                     */

                    double[] inputs =
                            dictionary
                                    .toBinaryVector(
                                            sentence
                                    );


                    /*
                     * Calculate prediction.
                     */

                    double[] probabilities =
                            calculateOutput(
                                    inputs
                            );


                    /*
                     * ============================
                     * LOSS
                     * ============================
                     *
                     * Cross entropy:
                     *
                     * loss = -log(correctProbability)
                     */

                    double correctProbability =
                            Math.max(
                                    probabilities[
                                            correctCategory
                                            ],
                                    0.0000001
                            );


                    double loss =
                            -Math.log(
                                    correctProbability
                            );


                    totalLoss +=
                            loss;


                    exampleCount++;


                    /*
                     * ============================
                     * BACKPROPAGATION
                     * ============================
                     */

                    for (
                            int output = 0;
                            output
                                    < categories.length;
                            output++
                    ) {


                        double target;


                        if (
                                output
                                        ==
                                correctCategory
                        ) {

                            target =
                                    1.0;

                        } else {

                            target =
                                    0.0;
                        }


                        /*
                         * Softmax + cross entropy
                         * gradient.
                         */

                        double gradient =
                                probabilities[output]
                                        - target;


                        /*
                         * Update weights.
                         */

                        for (
                                int input = 0;
                                input
                                        < inputs.length;
                                input++
                        ) {

                            weights[output][input]
                                    -=
                                    learningRate
                                            *
                                    gradient
                                            *
                                    inputs[input];
                        }


                        /*
                         * Update bias.
                         */

                        biases[output]
                                -=
                                learningRate
                                        *
                                gradient;
                    }
                }
            }


            /*
             * Show training progress.
             */

            if (
                    epoch % 250 == 0
            ) {

                double averageLoss =
                        totalLoss
                                /
                        exampleCount;


                System.out.println(
                        "Epoch: "
                                + epoch
                                + " | Loss: "
                                + averageLoss
                );
            }
        }


        System.out.println(
                "Training complete!"
        );
    }


    /*
     * =========================================
     * THINK
     * =========================================
     */

    public Answer think(
            String message
    ) {

        /*
         * Dictionary converts the user's
         * actual message into neural inputs.
         */

        double[] inputs =
                dictionary
                        .toBinaryVector(
                                message
                        );


        /*
         * Determine if the sentence contains
         * any recognized words.
         */

        boolean containsKnownWord =
                false;


        for (
                int i = 0;
                i < inputs.length;
                i++
        ) {

            /*
             * Don't count the UNKNOWN token.
             */

            if (
                    i
                            !=
                    dictionary
                            .getUnknownId()
                    &&
                    inputs[i]
                            > 0
            ) {

                containsKnownWord =
                        true;

                break;
            }
        }


        /*
         * No recognized words.
         */

        if (!containsKnownWord) {

            return new Answer(
                    "I don't know enough words to understand that yet.",
                    "UNKNOWN",
                    0.0
            );
        }


        /*
         * Run neural network.
         */

        double[] probabilities =
                calculateOutput(
                        inputs
                );


        /*
         * Find highest probability.
         */

        int bestCategory =
                0;


        for (
                int i = 1;
                i < probabilities.length;
                i++
        ) {

            if (
                    probabilities[i]
                            >
                    probabilities[
                            bestCategory
                            ]
            ) {

                bestCategory =
                        i;
            }
        }


        double confidence =
                probabilities[
                        bestCategory
                        ];


        /*
         * If confidence is too low,
         * don't pretend we know.
         */

        if (
                confidence
                        < 0.40
        ) {

            return new Answer(
                    "I'm not sure what you mean yet.",
                    categories[
                            bestCategory
                            ],
                    confidence
            );
        }


        /*
         * Convert neural-network category
         * into actual text.
         */

        String text =
                createAnswer(
                        bestCategory
                );


        return new Answer(
                text,
                categories[
                        bestCategory
                        ],
                confidence
        );
    }


    /*
     * =========================================
     * CREATE RESPONSE
     * =========================================
     */

    private String createAnswer(
            int category
    ) {

        String name =
                categories[
                        category
                        ];


        switch (name) {

            case "GREETING":

                return "Hello!";


            case "HOW_ARE_YOU":

                return "I'm doing well. My neural network is running!";


            case "THANKS":

                return "You're welcome!";


            case "GOODBYE":

                return "Goodbye!";


            case "HELP":

                return "Sure. Tell me what you need help with.";


            case "NAME":

                return "I'm a neural-network AI written in Java.";


            case "YES":

                return "Okay!";


            case "NO":

                return "Alright.";


            case "CODING":

                return "I recognize that you're talking about programming or software engineering.";


            case "AI":

                return "You're talking about artificial intelligence or neural networks.";


            default:

                return "I'm not sure what to say.";
        }
    }


    /*
     * =========================================
     * DICTIONARY INFORMATION
     * =========================================
     */

    public int getDictionarySize() {

        return dictionary.size();
    }


    /*
     * =========================================
     * HEADLESS SELF TEST
     * =========================================
     *
     * This exercises the real trained network. It is useful on
     * machines that cannot open a Swing window:
     *
     *     java App --self-test
     */

    public static boolean runSelfTest() {

        Brain brain =
                new Brain();


        String[][] checks = {
                {"hello there", "GREETING"},
                {"how are you doing", "HOW_ARE_YOU"},
                {"thank you very much", "THANKS"},
                {"see you later", "GOODBYE"},
                {"please help me", "HELP"},
                {"what is your name", "NAME"},
                {"yes", "YES"},
                {"nope", "NO"},
                {"help with java code", "CODING"},
                {"teach me artificial intelligence", "AI"}
        };


        boolean passed =
                true;


        for (String[] check : checks) {

            Answer answer =
                    brain.think(check[0]);


            boolean correct =
                    check[1].equals(answer.category);


            passed &=
                    correct;


            System.out.printf(
                    Locale.US,
                    "%s | expected=%s actual=%s confidence=%.2f%%%n",
                    correct ? "PASS" : "FAIL",
                    check[1],
                    answer.category,
                    answer.confidence * 100.0
            );
        }


        Answer unknown =
                brain.think("xyzzy plugh");


        boolean rejectsUnknown =
                "UNKNOWN".equals(unknown.category);


        passed &=
                rejectsUnknown;


        System.out.println(
                (rejectsUnknown ? "PASS" : "FAIL")
                        + " | unknown-word rejection"
        );


        System.out.println(
                passed
                        ? "SELF TEST PASSED"
                        : "SELF TEST FAILED"
        );


        return passed;
    }


    /*
     * =========================================
     * ANSWER CLASS
     * =========================================
     */

    static class Answer {

        final String text;

        final String category;

        final double confidence;


        Answer(
                String text,
                String category,
                double confidence
        ) {

            this.text =
                    text;

            this.category =
                    category;

            this.confidence =
                    confidence;
        }
    }
}


/*
 * ==================================================
 *
 *                  DICTIONARY
 *
 * ==================================================
 */

class AppDictionary {

    /*
     * Special tokens.
     */

    public static final String PAD =
            "<PAD>";

    public static final String UNKNOWN =
            "<UNKNOWN>";

    public static final String START =
            "<START>";

    public static final String END =
            "<END>";


    /*
     * word -> ID
     */

    private final Map<String, Integer>
            wordToId =
            new LinkedHashMap<>();


    /*
     * ID -> word
     */

    private final List<String>
            idToWord =
            new ArrayList<>();


    /*
     * Word frequency.
     */

    private final Map<String, Integer>
            frequencies =
            new LinkedHashMap<>();


    /*
     * Word tokenizer.
     */

    private static final Pattern WORD_PATTERN =
            Pattern.compile(
                    "[a-zA-Z0-9]+(?:'[a-zA-Z0-9]+)?"
            );


    /*
     * =========================================
     * CONSTRUCTOR
     * =========================================
     */

    public AppDictionary() {

        addSpecialToken(
                PAD
        );

        addSpecialToken(
                UNKNOWN
        );

        addSpecialToken(
                START
        );

        addSpecialToken(
                END
        );
    }


    /*
     * =========================================
     * TOKENIZE TEXT
     * =========================================
     */

    public List<String> tokenize(
            String text
    ) {

        List<String> words =
                new ArrayList<>();


        if (
                text == null
                        ||
                text.isBlank()
        ) {

            return words;
        }


        String normalized =
                text.toLowerCase(
                        Locale.ROOT
                );


        Matcher matcher =
                WORD_PATTERN.matcher(
                        normalized
                );


        while (
                matcher.find()
        ) {

            words.add(
                    matcher.group()
            );
        }


        return words;
    }


    /*
     * =========================================
     * LEARN A SENTENCE
     * =========================================
     */

    public void learnSentence(
            String sentence
    ) {

        List<String> words =
                tokenize(
                        sentence
                );


        for (
                String word :
                words
        ) {

            addWord(
                    word
            );
        }
    }


    /*
     * =========================================
     * ADD WORD
     * =========================================
     */

    public int addWord(
            String word
    ) {

        word =
                normalizeWord(
                        word
                );


        if (
                word.isEmpty()
        ) {

            return getUnknownId();
        }


        Integer existing =
                wordToId.get(
                        word
                );


        if (
                existing
                        !=
                null
        ) {

            frequencies.put(
                    word,

                    frequencies
                            .getOrDefault(
                                    word,
                                    0
                            )
                            + 1
            );


            return existing;
        }


        int newId =
                idToWord.size();


        wordToId.put(
                word,
                newId
        );


        idToWord.add(
                word
        );


        frequencies.put(
                word,
                1
        );


        return newId;
    }


    /*
     * =========================================
     * BINARY VECTOR
     * =========================================
     */

    public double[] toBinaryVector(
            String text
    ) {

        double[] vector =
                new double[
                        size()
                        ];


        List<String> words =
                tokenize(
                        text
                );


        for (
                String word :
                words
        ) {

            Integer id =
                    wordToId.get(
                            word
                    );


            /*
             * Known word.
             */

            if (
                    id
                            !=
                    null
            ) {

                vector[id] =
                        1.0;

            } else {

                /*
                 * Unknown word.
                 */

                vector[
                        getUnknownId()
                        ] =
                        1.0;
            }
        }


        return vector;
    }


    /*
     * =========================================
     * ENCODE
     * =========================================
     */

    public int[] encode(
            String text
    ) {

        List<String> words =
                tokenize(
                        text
                );


        int[] tokens =
                new int[
                        words.size()
                        ];


        for (
                int i = 0;
                i < words.size();
                i++
        ) {

            tokens[i] =
                    getId(
                            words.get(i)
                    );
        }


        return tokens;
    }


    /*
     * =========================================
     * DECODE
     * =========================================
     */

    public String decode(
            int[] tokens
    ) {

        if (
                tokens == null
        ) {

            return "";
        }


        StringBuilder builder =
                new StringBuilder();


        for (
                int token :
                tokens
        ) {

            String word =
                    getWord(
                            token
                    );


            if (
                    word.equals(PAD)
                            ||
                    word.equals(START)
                            ||
                    word.equals(END)
            ) {

                continue;
            }


            if (
                    builder.length()
                            > 0
            ) {

                builder.append(
                        " "
                );
            }


            builder.append(
                    word
            );
        }


        return builder.toString();
    }


    /*
     * =========================================
     * WORD LOOKUP
     * =========================================
     */

    public int getId(
            String word
    ) {

        word =
                normalizeWord(
                        word
                );


        Integer id =
                wordToId.get(
                        word
                );


        if (
                id == null
        ) {

            return getUnknownId();
        }


        return id;
    }


    /*
     * =========================================
     * TOKEN LOOKUP
     * =========================================
     */

    public String getWord(
            int id
    ) {

        if (
                id < 0
                        ||
                id >= idToWord.size()
        ) {

            return UNKNOWN;
        }


        return idToWord.get(
                id
        );
    }


    /*
     * =========================================
     * CONTAINS WORD
     * =========================================
     */

    public boolean contains(
            String word
    ) {

        return wordToId.containsKey(
                normalizeWord(
                        word
                )
        );
    }


    /*
     * =========================================
     * SIZE
     * =========================================
     */

    public int size() {

        return idToWord.size();
    }


    /*
     * =========================================
     * FREQUENCY
     * =========================================
     */

    public int getFrequency(
            String word
    ) {

        return frequencies
                .getOrDefault(
                        normalizeWord(
                                word
                        ),
                        0
                );
    }


    /*
     * =========================================
     * UNKNOWN TOKEN
     * =========================================
     */

    public int getUnknownId() {

        return wordToId.get(
                UNKNOWN
        );
    }


    public int getPadId() {

        return wordToId.get(
                PAD
        );
    }


    public int getStartId() {

        return wordToId.get(
                START
        );
    }


    public int getEndId() {

        return wordToId.get(
                END
        );
    }


    /*
     * =========================================
     * SPECIAL TOKEN
     * =========================================
     */

    private void addSpecialToken(
            String token
    ) {

        int id =
                idToWord.size();


        wordToId.put(
                token,
                id
        );


        idToWord.add(
                token
        );


        frequencies.put(
                token,
                0
        );
    }


    /*
     * =========================================
     * NORMALIZE WORD
     * =========================================
     */

    private String normalizeWord(
            String word
    ) {

        if (
                word == null
        ) {

            return "";
        }


        return word
                .toLowerCase(
                        Locale.ROOT
                )
                .trim();
    }


    /*
     * =========================================
     * PRINT DICTIONARY
     * =========================================
     */

    public void printDictionary() {

        System.out.println(
                "========== DICTIONARY =========="
        );


        for (
                int id = 0;
                id < idToWord.size();
                id++
        ) {

            String word =
                    idToWord.get(
                            id
                    );


            System.out.println(
                    id
                            + " -> "
                            + word
                            + " | frequency="
                            + getFrequency(
                                    word
                            )
            );
        }


        System.out.println(
                "Total tokens: "
                        + size()
        );


        System.out.println(
                "================================"
        );
    }
}
