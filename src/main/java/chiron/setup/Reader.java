package chiron.setup;

/**
 * Reads the XML file and create the Workflow object
 */
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import nu.xom.*;

public class Reader {

    private Element database;

    /**
     * Reade a XML file and sets up the Workflow data structure
     *
     * @param filename The XML Filename with the concetual workflow structure
     * @return The readed workflow
     * @throws ParsingException
     * @throws ValidityException
     * @throws IOException
     */
    public Workflow readXML(String filename) throws ParsingException, ValidityException, IOException {
        Builder builder = new Builder();
        File xmlFile = new File(filename);
        Document doc = builder.build(xmlFile);
        Element workflowElement = doc.getRootElement().getFirstChildElement("Workflow");
        //workflow attributes
        String wfTag = workflowElement.getAttributeValue("tag");
        String wfdescription = workflowElement.getAttributeValue("description");
        Workflow workflow = new Workflow(wfTag, wfdescription);
        //grab the database element
        database = doc.getRootElement().getFirstChildElement("database");
        //get all activities
        Elements activityElements = workflowElement.getChildElements();
        for (int j = 0; j < activityElements.size(); j++) {
            Element elemen = activityElements.get(j);
            String activityName = elemen.getAttributeValue("tag");
            String activityType = elemen.getAttributeValue("type");
            String activityDescription = elemen.getAttributeValue("description");
            String templatedir = elemen.getAttributeValue("template");
            String activation = elemen.getAttributeValue("activation");
            String extractor = elemen.getAttributeValue("extractor");
            String operand = elemen.getAttributeValue("operand");
            String workload = elemen.getAttributeValue("workload");
            if (workload == null) {
                workload = "0.0";
            } else {
//                System.out.println("workload: " + workload);
            }
            Activity activity = new Activity(activityName, Activity.Type.valueOf(activityType.toUpperCase()), activityDescription, templatedir, activation, extractor, workload);
            workflow.activities.put(activityName, activity);
            HashMap<String, Relation> relations = new HashMap<String, Relation>();
            Elements elemens = elemen.getChildElements();
            for (int i = 0; i < elemens.size(); i++) {
                Element el = elemens.get(i);
                String elementName = el.getLocalName();
                if (elementName.equals("Relation")) {
                    String name = el.getAttributeValue("name");
                    String type = el.getAttributeValue("reltype");
                    String dependency = el.getAttributeValue("dependency");

                    Relation rel = new Relation(name, Relation.Type.valueOf(type.toUpperCase()));
                    if (dependency != null) {
                        rel.setDependency(workflow.activities.get(dependency));
                    }
                    if (rel.getType().equals(Relation.Type.OUTPUT)) {
                        activity.addOutputRelation(rel);
                    } else {
                        activity.addInputRelation(rel);
                    }
                    relations.put(name, rel);
                } else {
                    String name = el.getAttributeValue("name");
                    String input = el.getAttributeValue("input");
                    String output = el.getAttributeValue("output");
                    String type = el.getAttributeValue("type");
                    String places = el.getAttributeValue("decimalplaces");
                    String operation = el.getAttributeValue("operation");
                    String instrumented = el.getAttributeValue("instrumented");
                    Field field = new Field(name);
                    field.setFtype(type);
                    if (places != null) {
                        field.setDecimalplaces((int) Integer.valueOf(places));
                    }
                    if (operation != null) {
                        field.setFileoperation(operation);
                    }
                    if (instrumented != null) {
                        field.setInstrumented(instrumented);
                    }
                    if (input != null) {
                        Relation relation = relations.get(input);
                        if (relation == null) {
                            String line = System.getProperty("line.separator");
                            String message = "The activity " + activity.getTag()
                                    + " has the following relations: " + line;
                            for (String relName : relations.keySet()) {
                                message += relName + line;
                            }
                            message += "However, the field " + field.getFname() + "says it belongs to relation " + input + line;
                            message += "Please check your XML.";
                            throw new NullPointerException(message);
                        }
                        relation.addField(field);
                    }
                    if (output != null) {
                        Relation relation = relations.get(output);
                        if (relation == null) {
                            String line = System.getProperty("line.separator");
                            String message = "The activity " + activity.getTag()
                                    + " has the following relations:" + relations.keySet() + line;
                            message += "However, the field " + field.getFname() + " says it belongs to relation " + output + ".";
                            message += " Please check your XML.";
                            throw new NullPointerException(message);
                        }
                        relation.addField(field);
                    }
                }
            }

            if (operand != null) {
                String[] operands = operand.split(",");
                for (String op : operands) {
                    if (activity.getType().equals(Activity.Type.REDUCE)) {
                        activity.addOperand("AGREG_FIELD", op);
                        activity.checkAgregationField(op);
                    } else if (activity.getType().equals(Activity.Type.EVALUATE)) {
                        activity.addOperand("MINTERM", op);
                    }
                }
            }
        }
        return workflow;
    }

    public List<Site> readSites(String filename, String IP) throws ParsingException, ValidityException, IOException {
        Site s;
        List<Site> sites = new ArrayList<Site>();
        Builder builder = new Builder();
        File xmlFile = new File(filename);
        Document doc = builder.build(xmlFile);
        Element multisite = doc.getRootElement().getFirstChildElement("multisite");
        Elements siteElements = multisite.getChildElements();
        for (int i = 0; i < siteElements.size(); i++) {
            Element site = siteElements.get(i);
            if(site.getAttributeValue("role").equals("slave")){
                Element db = site.getChildElements("database").get(0);
                s = new Site(db.getAttributeValue("erver"),
                        db.getAttributeValue("port"),
                        db.getAttributeValue("name"),
                        db.getAttributeValue("username"),
                        db.getAttributeValue("password"));
                sites.add(s);
            }
        }
        return sites;
    }

    public String getDbAttribute(String attr) {
        if (database != null) {
            return database.getAttributeValue(attr);
        } else {
            throw new NullPointerException("The method getDbAttribute must be called only after the readXML method.");
        }

    }
}
