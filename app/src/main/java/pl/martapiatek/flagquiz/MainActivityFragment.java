package pl.martapiatek.flagquiz;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private  static final String TAG = "FlagQuiz Activity";
    private static final int FLAGS_IN_QUIZ = 10;

    private List<String> fileNameList; // nazwy plikow flag
    private List<String> quizCountriesList; //kraje biezacego quizu
    private Set<String> regionsSet; // obszary biezacego quizu
    private String correctAnswer; // poprawna nazwa kraju przypisana do biezacej flagi
    private int totalGuesses; //liczba prob odpowiedzi
    private int correctAnswers; //liczba poprawnych odpwiedzi
    private int guessRows; //liczba wierszy przyciskow odpowiedzi wyswietlanych na ekranie
    private SecureRandom random; // losowanie
    private Handler handler; //uzywany podczas opozniania ladowania kolejnej flagi
    private Animation shakeAnimation; //animacja blednej odpowiedzi

    private LinearLayout quizLinearLayout;
    private TextView questionNumberTextView; // numer biezacego pytania
    private ImageView flagImageView; //wyswietla flage
    private LinearLayout[] guessLinearLayouts; // wiersze przyciskow odpowiedzi
    private TextView answerTextView; // wyswietla poprawna odpowiedz


    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater,container,savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main,container,false);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        shakeAnimation = AnimationUtils.loadAnimation(getActivity(),R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); // animacja jest powtarzana 3 razy

        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);

        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);

        answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        //obiekty nasluchujace przyciski odpowiedzi
        for(LinearLayout row : guessLinearLayouts){
            for(int column = 0; column < row.getChildCount(); column++){
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        questionNumberTextView.setText(getString(R.string.question,1, FLAGS_IN_QUIZ));
        return view;
    }

    //aktualizuje zmnienna guessRows na podstawie wartosci SharedPreferences
public void updateGuessRows(SharedPreferences sharedPreferences){

    //ustal liczbe przyciskow odpowiedzi
    String choices = sharedPreferences.getString(MainActivity.CHOICES, null);
    guessRows = Integer.parseInt(choices) / 2;

    //ukryj wszystkie obiekty LinearLayout przyciskow odpowiedzi
    for(LinearLayout layout : guessLinearLayouts)
        layout.setVisibility(View.GONE);

    //wyswietl wlasciwe obe=iekty przyciskow odpowiedzi
    for(int row = 0; row < guessRows; row++)
        guessLinearLayouts[row].setVisibility(View.VISIBLE);

}

//aktualizuje obszary, ktore ma obejmowac quiz
    public void updateRegions(SharedPreferences sharedPreferences){
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    //przygotowuje i uruchamia kolejny quiz
    public void resetQuiz(){
        //AssetManager umożliwia uzyskanie nazw plików obrazow flag
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear();

        try{
            for(String region : regionsSet){

                //uzyskaj liste wszystkich plikow z flagami z danego obszaru
                String[] paths = assets.list(region);

                for(String path : paths)
                    fileNameList.add(path.replace(".png", ""));
            }
        }catch (IOException exception){
            Log.e(TAG, "Błąd ładowania plików obrazów", exception);
        }

        correctAnswers = 0;
        totalGuesses = 0;
        quizCountriesList.clear();

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        //dodaj losowe nazwy plikow flag do quizu
        while(flagCounter <= FLAGS_IN_QUIZ){
            int randomIndex = random.nextInt(numberOfFlags);

            String filename = fileNameList.get(randomIndex);

            if(!quizCountriesList.contains(filename)){
                quizCountriesList.add(filename);
                ++flagCounter;
            }
        }

        loadNextFlag(); //uruchom quiz ladujac pierwsza flage

    }

    //zaladuj kolejna flage po udzieleniu poprawnej odpowiedzi
    private void loadNextFlag() {

        //ustal nazwe pliku kolejnej flagi i usun ja z listy
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;
        answerTextView.setText("");

        //wyswietl numer biezacego pytania
        questionNumberTextView.setText(getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

        //odczytaj info o obszarze z nazwy kolejnego pliku
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        AssetManager assets = getActivity().getAssets();

        try {
            InputStream stream = assets.open(region + "/" + nextImage + ".png");

            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);

             animate(false); //animuj flage wprowadzana na ekran

        } catch (IOException exception) {
            Log.e(TAG, "Błąd ładowania " + nextImage, exception);
        }

        Collections.shuffle(fileNameList); //pomieszaj nazwy plikow

        //prawidlowa odpowiedz umiesc na koncu listy fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));


        //dodaj 2,4,6 lub 8 przyciskow
        for(int row = 0; row < guessRows; row++){

            for(int column = 0; column < guessLinearLayouts[row].getChildCount(); column++){

                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                //ustal nazwe kraju i przeksztalc ja na wyswietlany tekst
                String filename = fileNameList.get((row*2) + column);
                newGuessButton.setText(getCountryName(filename));

            }
        }

        //zastap losowo wtbrany przycisk poprawna odpowiedzia
        int row = random.nextInt(guessRows);
        int column = random.nextInt(2);
        LinearLayout randomRow = guessLinearLayouts[row];
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);

    }

    //parsuje nazwe pliku flagi i zwraca nazwe  panstwa
    private String getCountryName(String name){
        return name.substring(name.indexOf('-') + 1).replace('_',' ');
    }

    //animacja
    private void animate(boolean animateOut){

        //nie wyswietlaj animacji podczas umieszczania pierwszej flagi
        if(correctAnswers == 0)
            return;

        //oblicz wspolrzedne x i y srodka
        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) / 2;
        int centerY = (quizLinearLayout.getTop() + quizLinearLayout.getBottom()) / 2;

        //oblicz promien animacji
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        Animator animator;

        if(animateOut){

            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, radius, 0);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                   loadNextFlag();
                }
            });

        }
        else {
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, 0, radius);

        }
        animator.setDuration(500); // czas animacji w ms
        animator.start();//uruchom animacje

    }

    private View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Button guessButton = ((Button) v);
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGuesses;

            //jesli odpowiedz jest poprawna
            if (guess.equals(answer)) {
                ++correctAnswers;

                //poprawna odpowiedz wyswietl zielona czcionka
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer, getContext().getTheme()));

                disableButtons(); // dezaktywuj wszystkie przyciski

                if (correctAnswers == FLAGS_IN_QUIZ) {
                    DialogFragment quizResults = new DialogFragment() {

                        @Override
                        public Dialog onCreateDialog(Bundle bundle) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(getString(R.string.results, totalGuesses, (1000 / (double) totalGuesses)));

                            //przycisk resetuj quiz
                            builder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    resetQuiz();
                                }
                            });
                            return builder.create(); //zwroc AlertDialog

                        }
                    };

                    quizResults.setCancelable(false);
                    quizResults.show(getFragmentManager(), "quiz results");

                } else {
                    handler.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    animate(true);
                                }
                            }, 2000);
                }
            }
            //odpowiedz nie jest poprawna
            else {
                flagImageView.startAnimation(shakeAnimation);

                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer, getContext().getTheme()));
                guessButton.setEnabled(false);


            }
        }

    };

    private void disableButtons(){
        for(int row = 0; row < guessRows; row++){
            LinearLayout guessRow = guessLinearLayouts[row];
            for(int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }

    }



