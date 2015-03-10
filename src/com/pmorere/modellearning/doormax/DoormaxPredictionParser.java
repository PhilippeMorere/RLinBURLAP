package com.pmorere.modellearning.doormax;

import burlap.oomdp.core.Domain;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.pmorere.modellearning.doormax.DoormaxModel.EffectTypes;

/**
 * Created by philippe on 23/02/15.
 */
public class DoormaxPredictionParser {

    public static void rulesToFile(List<DoormaxModel.Prediction> preds, String filePath) {
        // Get an unused file name
        File file = new File(filePath + "rules.txt");
        int i = 0;
        while (file.exists()) {
            file = new File(filePath + "rules" + i + ".txt");
            i++;
        }

        // Write the file
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (DoormaxModel.Prediction pred : preds)
                bw.write(predictionToString(pred) + "\n");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String predictionToString(DoormaxModel.Prediction pred) {
        StringBuffer sb = new StringBuffer();
        sb.append(pred.cond).append("#");
        sb.append(pred.eff).append("#");
        sb.append(pred.act.actionName()).append("#");
        sb.append(Arrays.toString(pred.act.params)).append("#");
        sb.append(pred.cumulatedReward).append("#");
        sb.append(pred.nbTries);
        return sb.toString();
    }

    public static List<DoormaxModel.Prediction> rulesFromFile(Domain domain, DoormaxModel.Prediction prediction, String filePath) {
        List<DoormaxModel.Prediction> preds = new ArrayList<DoormaxModel.Prediction>();

        // Get the last version of the file
        File file = new File(filePath + "rules.txt");
        int i = 0;
        File newFile = file;
        while (newFile.exists()) {
            file = newFile;
            newFile = new File(filePath + "rules" + i + ".txt");
            i++;
        }

        // Read the file
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = br.readLine()) != null)
                preds.add(predictionFromString(domain, prediction.copy(), line));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return preds;
    }

    private static DoormaxModel.Prediction predictionFromString(Domain domain, DoormaxModel.Prediction pred, String str) {
        String items[] = str.split("#");

        String items0[] = items[0].substring(items[0].indexOf('[') + 1, items[0].lastIndexOf(']')).split(",");
        int[] terms = new int[items0.length];
        for (int i = 0; i < items0.length; i++)
            if (items0[i].equals("*"))
                terms[i] = -1;
            else
                terms[i] = Integer.valueOf(items0[i]);
        pred.cond.terms = terms;


        String items1[] = items[1].substring(items[1].indexOf('{') + 1, items[1].lastIndexOf('}')).split(",");
        for (String pair : items1) {
            String[] keyValue = pair.split("=");
            if (keyValue[0].trim().equals("type"))
                pred.eff.type = EffectTypes.valueOf(keyValue[1]);
            else if (keyValue[0].trim().equals("attName"))
                pred.eff.attName = keyValue[1].substring(1, keyValue[1].length() - 1);
            else if (keyValue[0].trim().equals("objectName"))
                pred.eff.objectName = keyValue[1].substring(1, keyValue[1].length() - 1);
        }

        Action act = domain.getAction(items[2]);
        String actParamsStr = items[3].substring(1, items[3].length() - 1);
        String[] actParams = new String[0];
        if (!actParamsStr.isEmpty())
            actParams = actParamsStr.split(",");
        pred.act = new GroundedAction(act, actParams);

        pred.cumulatedReward = Double.valueOf(items[4]);

        pred.nbTries = Integer.valueOf(items[5]);

        return pred;
    }
}
