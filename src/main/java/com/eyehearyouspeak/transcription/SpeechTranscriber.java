package com.eyehearyouspeak.transcription;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import com.google.common.collect.Lists;
import com.google.cloud.storage.Storage;

import javax.sound.sampled.AudioInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class SpeechTranscriber
{
    private SpeechClient speech_client;
    private RecognitionConfig config;

    public SpeechTranscriber(String json_path, int num_speakers) throws IOException
    {

        // Define the location of the google credentials
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(json_path))
                .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
        Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();

        // Create the speech client
        this.speech_client = SpeechClient.create();

        // Define the diarization config
        SpeakerDiarizationConfig speakerDiarizationConfig =
                SpeakerDiarizationConfig.newBuilder()
                        .setEnableSpeakerDiarization(true)
                        .build();

        // Builds the sync recognize request
        this.config =
                RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setLanguageCode("en-US")
                        .setSampleRateHertz(16000)
                        .setDiarizationConfig(speakerDiarizationConfig)
                        .setUseEnhanced(true)
                        .setModel("video")
                        .build();
    }

    public ArrayList<String> transcribeAudio(AudioInputStream audio) throws IOException
    {
        System.out.println("Transcribing audio");
        ArrayList<String> diarized_text = new ArrayList<>();

        // Convert audio data to ByteString
        audio.reset();
        ByteString data = ByteString.copyFrom(audio.readAllBytes());

        // Create the object that will be sent to the stt service
        RecognitionAudio recognition_audio = RecognitionAudio.newBuilder().setContent(data).build();

        // Create the stt request
        RecognizeRequest request =
                RecognizeRequest.newBuilder().setConfig(this.config).setAudio(recognition_audio).build();

        // Get the response of the stt
        RecognizeResponse response = this.speech_client.recognize(request);

        // Grab the last alternative, as this is the one that contains the diarization information
        SpeechRecognitionAlternative alternative = response.getResults(response.getResultsCount() - 1).getAlternatives(0);

        // Add words to the string until the speaker tag changes. Then switch to a new string
        WordInfo word_info = alternative.getWords(0);
        int current_speaker_tag = word_info.getSpeakerTag();
        StringBuilder current_sentence = new StringBuilder(word_info.getWord());
        for (int i = 1; i < alternative.getWordsCount(); i++)
        {
            word_info = alternative.getWords(i);
            // If the speaker is the same
            if (current_speaker_tag == word_info.getSpeakerTag())
            {
                current_sentence.append(" ").append(word_info.getWord());
            }
            // If the speaker is different
            else
            {
                // Add the full sentence to the list
                diarized_text.add(String.valueOf(current_sentence));
                // Change the current speaker tag
                current_speaker_tag = word_info.getSpeakerTag();
                // Reset the string
                current_sentence = new StringBuilder();
            }
        }

        // Add the last sentence
        diarized_text.add(String.valueOf(current_sentence));

        return diarized_text;
    }
}
