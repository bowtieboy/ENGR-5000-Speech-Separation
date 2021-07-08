// TODO: Implement threading into this class to improve performance
public class DigitalFilters
{
    // Used for filtering 16kHz down to 8kHz. Fstop is 4kHz
    private static final double[] lowpass_8k = new double[] {
            0.004107475681421,  0.02333483941182, 0.004297307448838,-0.001739162251989,
            -0.003062118265377, 0.001833831978175, 0.002215802625304,-0.001832855098479,
            -0.001984720463481, 0.001880798308636, 0.001939162120181,-0.001928979353986,
            -0.001983127885456, 0.001969678123036,  0.00207930546545,-0.002001190918542,
            -0.002207768441941, 0.002023700329109, 0.002360190169781,-0.002039123246802,
            -0.002534165713242, 0.002042782783106, 0.002721043047481,-0.002038262299179,
            -0.002924897634582, 0.002022479440659, 0.003140454433307,-0.001995263848649,
            -0.003369415263144, 0.001956126934387, 0.003610837670472,-0.001903278333997,
            -0.003865975614946, 0.001836942024095, 0.004135722611827,-0.001755491923376,
            -0.004419379276132, 0.001660814371083,  0.00471937755204,-0.001549051865212,
            -0.005032622139562, 0.001420029659568, 0.005362411113878,-0.001270731382089,
            -0.005708860117181, 0.001096163066426, 0.006077278887983,-0.0008984135570158,
            -0.006466859084448,0.0006728754673783, 0.006883435420327,-0.000422586012181,
            -0.007325429312888, 0.000131030888747, 0.007784990045841, 0.000186315069385,
            -0.008305175876907,-0.0005782495775835, 0.008837914765242,0.0009940210360832,
            -0.009427998947814,-0.001462685070763,  0.01007918615064, 0.002013342760894,
            -0.01080474351669,-0.002651534095534,  0.01161956669087, 0.003387948513042,
            -0.01253090275647,-0.004267927162332,  0.01358898203956, 0.005300424527334,
            -0.01481734185776,-0.006554114798989,   0.0163004567918, 0.008105459515639,
            -0.01811346179335,  -0.0100651732138,  0.02042102893016,  0.01263554228593,
            -0.02347110940646, -0.01615741555197,  0.02773863213942,  0.02130034496626,
            -0.03420083637395,  -0.0295532276073,  0.04525442251565,  0.04505417040653,
            -0.06877606139057, -0.08518453050043,   0.1546519252177,   0.4454317154424,
            0.4454317154424,   0.1546519252177, -0.08518453050043, -0.06877606139057,
            0.04505417040653,  0.04525442251565,  -0.0295532276073, -0.03420083637395,
            0.02130034496626,  0.02773863213942, -0.01615741555197, -0.02347110940646,
            0.01263554228593,  0.02042102893016,  -0.0100651732138, -0.01811346179335,
            0.008105459515639,   0.0163004567918,-0.006554114798989, -0.01481734185776,
            0.005300424527334,  0.01358898203956,-0.004267927162332, -0.01253090275647,
            0.003387948513042,  0.01161956669087,-0.002651534095534, -0.01080474351669,
            0.002013342760894,  0.01007918615064,-0.001462685070763,-0.009427998947814,
            0.0009940210360832, 0.008837914765242,-0.0005782495775835,-0.008305175876907,
            0.000186315069385, 0.007784990045841, 0.000131030888747,-0.007325429312888,
            -0.000422586012181, 0.006883435420327,0.0006728754673783,-0.006466859084448,
            -0.0008984135570158, 0.006077278887983, 0.001096163066426,-0.005708860117181,
            -0.001270731382089, 0.005362411113878, 0.001420029659568,-0.005032622139562,
            -0.001549051865212,  0.00471937755204, 0.001660814371083,-0.004419379276132,
            -0.001755491923376, 0.004135722611827, 0.001836942024095,-0.003865975614946,
            -0.001903278333997, 0.003610837670472, 0.001956126934387,-0.003369415263144,
            -0.001995263848649, 0.003140454433307, 0.002022479440659,-0.002924897634582,
            -0.002038262299179, 0.002721043047481, 0.002042782783106,-0.002534165713242,
            -0.002039123246802, 0.002360190169781, 0.002023700329109,-0.002207768441941,
            -0.002001190918542,  0.00207930546545, 0.001969678123036,-0.001983127885456,
            -0.001928979353986, 0.001939162120181, 0.001880798308636,-0.001984720463481,
            -0.001832855098479, 0.002215802625304, 0.001833831978175,-0.003062118265377,
            -0.001739162251989, 0.004297307448838,  0.02333483941182, 0.004107475681421
    };

    // Bandpass for filtering 44100kHz down to 16kHz with cutoff freqs. at 100Hz and 4kHz.
    private static final double[] bandpass_16k = new double[] {
            0.008000081964732,  0.03359694790965, -0.01820424180897,  0.00677388491416,
            0.004885052396537,0.0004966422708069,-0.001416284153845, 0.001828625969626,
            0.003897281198517, 0.001639756472806,-0.000532570340438, 0.001220396562921,
            0.003536059947245, 0.002238701663748,-0.0001224676555061,0.0008979548003354,
            0.003361268646117, 0.002659310789527, 0.000170426450942,0.0006657555135636,
            0.003224584790561, 0.002993398909586, 0.000419854906781,0.0004681060171858,
            0.003084432884448, 0.003265714638303,0.0006401201264125,0.0002735992590572,
            0.002910857869305, 0.003486208953795,0.0008371950331073,6.055618322818e-05,
            0.002682191074431, 0.003652536832604, 0.001041550789912,-0.000129174277679,
            0.002423914828527, 0.003768049184124, 0.001218575935761,-0.0003481696991341,
            0.002104767649133, 0.003828275434697, 0.001391125043596,-0.0005641088573934,
            0.00173733031243, 0.003829202694862,  0.00155167564611,-0.0007852364224278,
            0.001314379148396, 0.003766501734418, 0.001697236192027,-0.001011181377324,
            0.000839398848012, 0.003634777643129, 0.001821277703376,-0.001237724655904,
            0.000313680041937, 0.003433441778532, 0.001927104553061,-0.001463000414779,
            -0.0002602188238912, 0.003158955027114, 0.002010630346258,-0.001680723981289,
            -0.0008817415826326, 0.002801962612998, 0.002061324189483, -0.00189405559175,
            -0.001540431103644,  0.00237206777817, 0.002085714362848,-0.002092898165543,
            -0.002235405788382,  0.00185449057107,   0.0020728220632, -0.00227012357231,
            -0.00296880228853, 0.001252124302734, 0.002044871215432,-0.002444958611535,
            -0.003733355183922,0.0006039976101132, 0.001944082418988,-0.002594144210224,
            -0.004477681446079,-0.0001703278353915, 0.001833416321326,-0.002719898207197,
            -0.005248371371281,-0.0009978729647825, 0.001655577440266,-0.002796848690782,
            -0.006024318678035,-0.001901162850208, 0.001434318937406,-0.002840682725313,
            -0.006781654908956,-0.002879719807896, 0.001164480648975,-0.002845494373016,
            -0.00751883104793,-0.003932279637059,0.0008435239398063,-0.002803919217569,
            -0.008216608249766,-0.005053190382989,0.0004670536669729,-0.002708326551107,
            -0.008871743206769,-0.006237039797125,2.927584200575e-05,-0.002555092651054,
            -0.009462961086964, -0.00748602470035,-0.000472295116194,-0.002333335563511,
            -0.009984108455573,-0.008792953944574,-0.001040763463632,-0.002039249098357,
            -0.01041303521049, -0.01015916625932,-0.001688757369214, -0.00165994429234,
            -0.01073341606669,   -0.011588590403,-0.002422884328615,-0.001181881079274,
            -0.01092664355558, -0.01308441180194,-0.003253659003169,-0.0005883164987039,
            -0.0109647696048, -0.01465843288844, -0.00420581718796,0.0001446765064284,
            -0.01081621937623, -0.01633109295671,-0.005305030677154, 0.001053972107338,
            -0.01043268133602, -0.01813762359101,-0.006600902444473, 0.002195260435071,
            -0.009748367988261, -0.02014036336426,-0.008167828800679, 0.003656968744581,
            -0.008654796326541, -0.02244933799558,   -0.010143241354,  0.00559661112725,
            -0.006958649371325, -0.02527422801762, -0.01278234354972, 0.008324550565331,
            -0.004296180427747, -0.02905729085579, -0.01661031089801,  0.01249795569006,
            0.0001615432660243, -0.03483308868232, -0.02300031861741,  0.01990521702752,
            0.008834985980301, -0.04592718896233, -0.03665767354373,  0.03754708766195,
            0.03266544301403, -0.08079257313948, -0.09252554909779,   0.1471103202678,
            0.4331936094405,   0.4331936094405,   0.1471103202678, -0.09252554909779,
            -0.08079257313948,  0.03266544301403,  0.03754708766195, -0.03665767354373,
            -0.04592718896233, 0.008834985980301,  0.01990521702752, -0.02300031861741,
            -0.03483308868232,0.0001615432660243,  0.01249795569006, -0.01661031089801,
            -0.02905729085579,-0.004296180427747, 0.008324550565331, -0.01278234354972,
            -0.02527422801762,-0.006958649371325,  0.00559661112725,   -0.010143241354,
            -0.02244933799558,-0.008654796326541, 0.003656968744581,-0.008167828800679,
            -0.02014036336426,-0.009748367988261, 0.002195260435071,-0.006600902444473,
            -0.01813762359101, -0.01043268133602, 0.001053972107338,-0.005305030677154,
            -0.01633109295671, -0.01081621937623,0.0001446765064284, -0.00420581718796,
            -0.01465843288844,  -0.0109647696048,-0.0005883164987039,-0.003253659003169,
            -0.01308441180194, -0.01092664355558,-0.001181881079274,-0.002422884328615,
            -0.011588590403, -0.01073341606669, -0.00165994429234,-0.001688757369214,
            -0.01015916625932, -0.01041303521049,-0.002039249098357,-0.001040763463632,
            -0.008792953944574,-0.009984108455573,-0.002333335563511,-0.000472295116194,
            -0.00748602470035,-0.009462961086964,-0.002555092651054,2.927584200575e-05,
            -0.006237039797125,-0.008871743206769,-0.002708326551107,0.0004670536669729,
            -0.005053190382989,-0.008216608249766,-0.002803919217569,0.0008435239398063,
            -0.003932279637059, -0.00751883104793,-0.002845494373016, 0.001164480648975,
            -0.002879719807896,-0.006781654908956,-0.002840682725313, 0.001434318937406,
            -0.001901162850208,-0.006024318678035,-0.002796848690782, 0.001655577440266,
            -0.0009978729647825,-0.005248371371281,-0.002719898207197, 0.001833416321326,
            -0.0001703278353915,-0.004477681446079,-0.002594144210224, 0.001944082418988,
            0.0006039976101132,-0.003733355183922,-0.002444958611535, 0.002044871215432,
            0.001252124302734, -0.00296880228853, -0.00227012357231,   0.0020728220632,
            0.00185449057107,-0.002235405788382,-0.002092898165543, 0.002085714362848,
            0.00237206777817,-0.001540431103644, -0.00189405559175, 0.002061324189483,
            0.002801962612998,-0.0008817415826326,-0.001680723981289, 0.002010630346258,
            0.003158955027114,-0.0002602188238912,-0.001463000414779, 0.001927104553061,
            0.003433441778532, 0.000313680041937,-0.001237724655904, 0.001821277703376,
            0.003634777643129, 0.000839398848012,-0.001011181377324, 0.001697236192027,
            0.003766501734418, 0.001314379148396,-0.0007852364224278,  0.00155167564611,
            0.003829202694862,  0.00173733031243,-0.0005641088573934, 0.001391125043596,
            0.003828275434697, 0.002104767649133,-0.0003481696991341, 0.001218575935761,
            0.003768049184124, 0.002423914828527,-0.000129174277679, 0.001041550789912,
            0.003652536832604, 0.002682191074431,6.055618322818e-05,0.0008371950331073,
            0.003486208953795, 0.002910857869305,0.0002735992590572,0.0006401201264125,
            0.003265714638303, 0.003084432884448,0.0004681060171858, 0.000419854906781,
            0.002993398909586, 0.003224584790561,0.0006657555135636, 0.000170426450942,
            0.002659310789527, 0.003361268646117,0.0008979548003354,-0.0001224676555061,
            0.002238701663748, 0.003536059947245, 0.001220396562921,-0.000532570340438,
            0.001639756472806, 0.003897281198517, 0.001828625969626,-0.001416284153845,
            0.0004966422708069, 0.004885052396537,  0.00677388491416, -0.01820424180897,
            0.03359694790965, 0.008000081964732
    };

    public static float[] applyBandpassFilter(float[] audio)
    {
        return applyFIRFilter(audio, bandpass_16k);
    }

    public static float[] applyLowpassFilter(float[] audio)
    {
        return applyFIRFilter(audio, lowpass_8k);
    }

    /**
     * @param input: Array of values that the filter will be applied to
     * @param impulse_response: Coefficients used by the FIR filter
     * @return: Array of values that have been passed through the filter
     */
    private static float[] applyFIRFilter(float[] input, double[] impulse_response)
    {
        // Variables used for applying filtering
        int length = impulse_response.length;
        float[] delay_line = new float[length];
        int count = 0;

        // Final return variable
        float[] output = new float[input.length];

        for (int i = 0; i < input.length; i++)
        {
            output[i] = getOutputSample(input[i], length, delay_line, impulse_response, count);
            if (++count >= length)
            {
                count = 0;
            }
        }

        return output;
    }

    /**
     * @param input_sample: Sample that will be passed through the filter
     * @param length: length of the impulse response
     * @param delay_line: circular line for FIR calculation
     * @param impulse_response: Coefficients used by the FIR filter
     * @param count: How many samples have been passed through. Resets when count==impulse_response.length. Might have
     *             bug where count is not being incremented, will need testing when function is actually used
     * @return: The filtered sample
     */
    private static float getOutputSample(float input_sample, int length, float[] delay_line, double[] impulse_response, int count)
    {
        delay_line[count] = input_sample;
        float result = 0.0f;
        int index = count;
        for (int i=0; i<length; i++)
        {
            result += impulse_response[i] * delay_line[index--];
            if (index < 0) index = length-1;
        }
        return result;
    }
}
