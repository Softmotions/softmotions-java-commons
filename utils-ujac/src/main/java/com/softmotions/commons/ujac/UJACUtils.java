package com.softmotions.commons.ujac;

import bsh.EvalError;
import bsh.Interpreter;
import com.softmotions.commons.string.EscapeHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ujac.util.exi.BaseExpressionOperation;
import org.ujac.util.exi.ExpressionContext;
import org.ujac.util.exi.ExpressionException;
import org.ujac.util.exi.ExpressionInterpreter;
import org.ujac.util.exi.ExpressionOperation;
import org.ujac.util.exi.ExpressionTuple;
import org.ujac.util.exi.NoOperandException;
import org.ujac.util.exi.Operand;
import org.ujac.util.exi.type.CollectionType;
import org.ujac.util.exi.type.DefaultType;
import org.ujac.util.exi.type.DoubleType;
import org.ujac.util.exi.type.FloatType;
import org.ujac.util.exi.type.IntegerType;
import org.ujac.util.exi.type.LongType;
import org.ujac.util.exi.type.NullValueType;
import org.ujac.util.exi.type.ShortType;
import org.ujac.util.exi.type.StringType;
import org.ujac.util.template.DefaultTemplateInterpreter;
import org.ujac.util.template.DefaultTemplateInterpreterFactory;
import org.ujac.util.template.TemplateContext;
import org.ujac.util.template.TemplateException;
import org.ujac.util.template.TemplateInterpreterFactory;
import org.ujac.util.text.BigDecimalFormat;
import org.ujac.util.text.FormatHelper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

/**
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id$
 */
public class UJACUtils {

    private static final Logger log = LoggerFactory.getLogger(UJACUtils.class);

    private static final TemplateInterpreterFactory TIF = new DefaultTemplateInterpreterFactory();

    public static FormatHelper getFormatHelper() {
        return getFormatHelper("#", "#.###", Locale.getDefault());
    }

    public static FormatHelper getFormatHelper(String intPattern, String doublePattern, Locale l) {
        FormatHelper fh = (l != null) ? FormatHelper.createInstance(l) : FormatHelper.createInstance();
        fh.setIntegerFormat(new BigDecimalFormat(intPattern));
        BigDecimalFormat df = new BigDecimalFormat(doublePattern);
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(dfs);
        fh.setDoubleFormat(df);
        return fh;
    }

    public static void processUJACTemplate(String t, Map props, Writer writer) throws IOException, TemplateException {
        DefaultTemplateInterpreter ti = new DefaultTemplateInterpreter(createExpressionInterpreter());
        TemplateContext ctx = TIF.createTemplateContext(props, getFormatHelper());
        ti.execute(t, writer, ctx);
    }


    public static String expandMap(String src, Map<String, String> props) {
        StringWriter res = new StringWriter();
        try {
            processUJACTemplate(src, System.getenv(), res);
        } catch (IOException | TemplateException e) {
            log.error("", e);
            return src;
        }
        return res.toString();
    }


    public static ExpressionInterpreter createExpressionInterpreter() {

        ExpressionInterpreter ei = new ExpressionInterpreter();

        FloatType floatType = new FloatType(ei);
        DoubleType doubleType = new DoubleType(ei, Double.class, "double");
        IntegerType intType = new IntegerType(ei, Integer.class, "int");
        LongType longType = new LongType(ei);
        ShortType shortType = new ShortType(ei);
        StringType stringType = new StringType(ei);
        DefaultType defaultType = new DefaultType(ei);
        NullValueType nullValueType = new NullValueType(ei);
        CollectionType collectionType = new CollectionType(ei);

        //Register Not zero custom operation
        NZExpressionOperation nzExpressionOperation = new NZExpressionOperation();
        floatType.addOperation("nz", nzExpressionOperation);
        doubleType.addOperation("nz", nzExpressionOperation);
        intType.addOperation("nz", nzExpressionOperation);
        longType.addOperation("nz", nzExpressionOperation);
        shortType.addOperation("nz", nzExpressionOperation);

        //Register operations for strings
        NNExpressionOperation nnExpressionOperation = new NNExpressionOperation();
        nullValueType.addOperation("nn", nnExpressionOperation);
        stringType.addOperation("nn", nnExpressionOperation);
        floatType.addOperation("nn", nnExpressionOperation);
        doubleType.addOperation("nn", nnExpressionOperation);
        intType.addOperation("nn", nnExpressionOperation);
        longType.addOperation("nn", nnExpressionOperation);
        shortType.addOperation("nn", nnExpressionOperation);

        stringType.addOperation("capitalize", new CapitalizeExpressionOperation());
        stringType.addOperation("decapitalize", new DecapitalizeExpressionOperation());

        //Register Printf format custom operation
        PrintfFormatExpressionOperation fmtExpressionOperation = new PrintfFormatExpressionOperation();
        floatType.addOperation("fmt", fmtExpressionOperation);
        doubleType.addOperation("fmt", fmtExpressionOperation);
        intType.addOperation("fmt", fmtExpressionOperation);
        longType.addOperation("fmt", fmtExpressionOperation);
        shortType.addOperation("fmt", fmtExpressionOperation);

        //Register Printf format custom operation
        RoundDivOperation roundDiv = new RoundDivOperation();
        intType.addOperation("roundDiv", roundDiv);
        longType.addOperation("roundDiv", roundDiv);
        shortType.addOperation("roundDiv", roundDiv);

        InvokeOperation invokeOperation = new InvokeOperation(ei);
        defaultType.addOperation("invoke", invokeOperation);
        collectionType.addOperation("invoke", invokeOperation);
        stringType.addOperation("invoke", invokeOperation);

        EscapeXMLOperation escxml = new EscapeXMLOperation();
        stringType.addOperation("xml", escxml);
        defaultType.addOperation("xml", escxml);
        nullValueType.addOperation("xml", escxml);

        SeparateLettersExpressionOperation slOp = new SeparateLettersExpressionOperation();
        stringType.addOperation("separateLetters", slOp);

        ei.registerTypeHandler(floatType);
        ei.registerTypeHandler(doubleType);
        ei.registerTypeHandler(intType);
        ei.registerTypeHandler(longType);
        ei.registerTypeHandler(shortType);
        ei.registerTypeHandler(stringType);
        ei.registerTypeHandler(defaultType);
        ei.registerTypeHandler(nullValueType);
        ei.registerTypeHandler(collectionType);

        return ei;
    }

    /**
     * Операция вставляет пробелы между всеми символами строки.
     * Полезна для оформления заголовков.
     */
    static class SeparateLettersExpressionOperation implements ExpressionOperation {

        public Object evaluate(ExpressionTuple expressionTuple, ExpressionContext expressionContext) throws ExpressionException {
            Operand obj = expressionTuple.getObject();
            if (obj == null) {
                throw new ExpressionException("Invalid operand");
            }

            String str = (String) obj.getValue();
            StringBuilder sb = new StringBuilder(str);
            int i;
            int k = 1;
            for (i = 0; i < str.length() - 1; i++) {
                if (Character.isLetter(str.charAt(i))) {
                    sb.insert(i + k, " ");
                    k++;
                }
            }
            return sb.toString();
        }

        public String getDescription() {
            return "Separate letters in strings";
        }

        public String getExamples() {
            return null;
        }
    }

    static class EscapeXMLOperation implements ExpressionOperation {

        public Object evaluate(ExpressionTuple expressionTuple, ExpressionContext expressionContext) throws ExpressionException {
            Operand operand = expressionTuple.getObject();
            if (operand == null || operand.getValue() == null) {
                return null;
            }
            String val = String.valueOf(operand.getValue());
            return EscapeHelper.escapeXML(val);
        }

        public String getDescription() {
            return "XML Escape operation";
        }

        public String getExamples() {
            return "";
        }
    }

    static class InvokeOperation extends BaseExpressionOperation {

        private ExpressionInterpreter interpreter;

        public InvokeOperation(ExpressionInterpreter interpreter) {
            this.interpreter = interpreter;
        }

        public Object evaluate(ExpressionTuple expr, ExpressionContext ctx) throws ExpressionException {

            Operand operand = expr.getOperand();
            if (operand == null) {
                throw new NoOperandException("No operand given for operation: " + expr.getOperation() +
                                             " on object " + expr.getObject() + "!");
            }
            String operandValue = (String) interpreter.evalOperand(operand, ctx);
            Object obj = expr.getObject().getValue();
            if (obj == null) {
                throw new ExpressionException("Object for operation: invoke cannot be null");
            }

            Interpreter bsh = new Interpreter();
            bsh.setClassLoader(Thread.currentThread().getContextClassLoader());
            Object res;
            try {
                bsh.set("obj", obj);
                res = bsh.eval("obj." + operandValue + ';');
            } catch (EvalError evalError) {
                throw new ExpressionException(evalError.getMessage(), evalError);
            }

            return res;
        }
    }


    static class RoundDivOperation extends BaseExpressionOperation {

        public Object evaluate(ExpressionTuple expr,
                               ExpressionContext ctx) throws ExpressionException {

            Operand operand = expr.getOperand();
            if (operand == null) {
                throw new NoOperandException("No operand given for operation: " + expr.getOperation() +
                                             " on object " + expr.getObject() + "!");
            }
            ExpressionInterpreter interpreter = ExpressionInterpreter.createInstance();
            // getting operand
            double operandValue = (double) interpreter.evalIntOperand(operand, ctx, false);
            double objval = ((Number) expr.getObject().getValue()).doubleValue();

            // division result of object through operand
            return (int) Math.round(objval / operandValue);
        }
    }


    static class PrintfFormatExpressionOperation implements ExpressionOperation {

        public Object evaluate(ExpressionTuple expressionTuple, ExpressionContext expressionContext) throws ExpressionException {
            Operand obj = expressionTuple.getObject();
            if (obj == null) {
                return null;
            }
            String format = String.valueOf(expressionTuple.getOperand().getValue());
            return String.format(format, obj);
        }

        public String getDescription() {
            return "Formatting like 'String.format(...)' implementation";
        }

        public String getExamples() {
            return null;
        }
    }


    /**
     * Данная операция предназначена для скрытия нулевых значений
     * Пример 1: ${11 nz}  -> "11"
     * Пример 2: ${0.0 nz} -> ""
     * Пример 3: ${null nz} -> ""
     */
    static class NZExpressionOperation implements ExpressionOperation {

        public Object evaluate(ExpressionTuple expressionTuple, ExpressionContext expressionContext) throws ExpressionException {
            Operand obj = expressionTuple.getObject();

            if (obj == null) {
                return "";
            }
            Object value = obj.getValue();
            if (((value instanceof Integer) && (((Integer) value) == 0)) ||
                ((value instanceof Float) && (((Float) value) == 0)) ||
                ((value instanceof Short) && (((Short) value) == 0)) ||
                ((value instanceof Double) && (((Double) value) == 0)) ||
                ((value instanceof Long) && (((Long) value) == 0))
                    ) {
                return "";
            }
            return value;
        }

        public String getDescription() {
            return "Not Zero (nz) Operation";
        }

        public String getExamples() {
            return null;
        }
    }

    /**
     * Операция предназначена для скрытия null значений
     * Пример 1: ${"11" nn}  -> "11"
     * Пример 2: ${null nn} -> ""
     */
    static class NNExpressionOperation implements ExpressionOperation {

        public Object evaluate(ExpressionTuple expr, ExpressionContext ctx) throws ExpressionException {

            Operand obj = expr.getObject();
            if (obj == null || obj.getValue() == null) {
                return "";
            } else {
                return obj.getValue();
            }
        }

        public String getDescription() {
            return "Not Null (nn) Operation";
        }

        public String getExamples() {
            return null;
        }
    }


    /**
     * Операция капитализации  для строк
     * (первый символ строки прописной)
     */
    static class CapitalizeExpressionOperation implements ExpressionOperation {

        public Object evaluate(ExpressionTuple expr, ExpressionContext expressionContext) throws ExpressionException {
            Operand obj = expr.getObject();
            if (obj == null) {
                return "";
            }
            String val = obj.getValue().toString();
            if (val.isEmpty()) {
                return "";
            }
            if (Character.isLowerCase(val.charAt(0)) &&
                (val.length() == 1 || Character.isLowerCase(val.charAt(1)))) {
                return Character.toUpperCase(val.charAt(0)) + val.substring(1);
            }
            return val;
        }

        public String getDescription() {
            return "String capitalization (capitalize) operation";
        }

        public String getExamples() {
            return null;
        }
    }


    static class DecapitalizeExpressionOperation implements ExpressionOperation {

        public Object evaluate(ExpressionTuple expr, ExpressionContext expressionContext) throws ExpressionException {
            Operand obj = expr.getObject();
            if (obj == null) {
                return "";
            }
            String val = obj.getValue().toString();
            if (val.isEmpty()) {
                return "";
            }
            if (Character.isUpperCase(val.charAt(0))) {
                return Character.toLowerCase(val.charAt(0)) + val.substring(1);
            }
            return val;
        }

        public String getDescription() {
            return "String decapitalization (decapitalize) operation";
        }

        public String getExamples() {
            return null;
        }
    }
}
