package pl.martapiatek.flagquiz;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
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
                //button.setOnClickListener(guessButtonListener);
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

      //  loadNextFlag(); //uruchom quiz ladujac pierwsza flage

    }


}
