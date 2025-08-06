package bog.bubblinator;

import cwlib.enums.*;
import cwlib.resources.RLevel;
import cwlib.resources.RPalette;
import cwlib.resources.RPlan;
import cwlib.structs.inventory.UserCreatedDetails;
import cwlib.structs.level.PlayerRecord;
import cwlib.structs.things.Thing;
import cwlib.structs.things.components.EggLink;
import cwlib.structs.things.components.LevelSettings;
import cwlib.structs.things.components.script.FieldLayoutDetails;
import cwlib.structs.things.components.script.InstanceLayout;
import cwlib.structs.things.components.script.ScriptInstance;
import cwlib.structs.things.components.script.ScriptObject;
import cwlib.structs.things.components.shapes.Polygon;
import cwlib.structs.things.parts.*;
import cwlib.types.Resource;
import cwlib.types.data.GUID;
import cwlib.types.data.NetworkPlayerID;
import cwlib.types.data.ResourceDescriptor;
import cwlib.types.data.Revision;
import cwlib.types.databases.FileDB;
import cwlib.types.databases.FileDBRow;
import cwlib.types.mods.Mod;
import cwlib.util.Resources;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.Sys;
import toolkit.functions.ModCallbacks;
import toolkit.utilities.FileChooser;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * @author Bog
 */
public class Bubblinator {
    private JButton loadPlanDescriptorsFromButton;
    public JList<String> planList;
    public JPanel mainForm;
    private JButton generatePLANButton;
    private JButton addPlanDescriptorButton;
    private JButton removeButton;
    public JCheckBox legacyFileDialogueCheckBox;
    private JCheckBox bubblebombCheckBox;
    private JCheckBox preferHashCheckBox;
    private JCheckBox shareableCheckBox;
    private JSpinner maxColumn;
    public JLabel entryCount;
    private JButton generateBubbleBINButton;
    private JButton clearAllButton;
    public status status;

    public Bubblinator()
    {
        maxColumn.setValue(25);
        loadPlanDescriptorsFromButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File[] files = FileChooser.openFile(null,"pal,mod,map", false, true);
                DefaultListModel<String> model = (DefaultListModel<String>) planList.getModel();

                for(File file : files)
                {
                    String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);

                    switch (ext.toLowerCase())
                    {
                        case "pal":
                            RPalette pal = new Resource(file.getAbsolutePath()).loadResource(RPalette.class);

                            for(ResourceDescriptor plan : pal.planList)
                            {
                                String descriptor = plan.isGUID() ? plan.getGUID().toString() : plan.getSHA1().toString();
                                System.out.println(descriptor + " was added to list.");
                                model.addElement(descriptor);
                            }
                            entryCount.setText("Entries: " + model.size());

                            break;
                        case "mod":

                            Mod mod = ModCallbacks.loadMod(file);

                            for(FileDBRow entry : ((FileDB)mod).entries)
                            {
                                String descriptor = "g" + String.valueOf(entry.getGUID()) + " : h" + entry.getSHA1().toString();
                                if(descriptor.startsWith("gg"))
                                    descriptor = descriptor.substring(1);

                                if(Resources.getResourceType(mod.extract(entry.getSHA1())) == ResourceType.PLAN)
                                {
                                    System.out.println(descriptor + " was added to list.");
                                    model.addElement(descriptor);
                                }
                            }
                            entryCount.setText("Entries: " + model.size());

                            break;
                        case "map":

                            FileDB fileDB = new FileDB(file);

                            for(FileDBRow entry : fileDB.entries)
                            {
                                String entryExt = entry.getPath().substring(entry.getPath().lastIndexOf(".") + 1);
                                String descriptor = "g" + String.valueOf(entry.getGUID()) + " : h" + entry.getSHA1().toString();
                                if(descriptor.startsWith("gg"))
                                    descriptor = descriptor.substring(1);

                                if(entryExt.equalsIgnoreCase("pln") || entryExt.equalsIgnoreCase("plan") || entry.getInfo().getType() == ResourceType.PLAN)
                                {
                                    System.out.println(descriptor + " was added to list.");
                                    model.addElement(descriptor);
                                }
                            }
                            entryCount.setText("Entries: " + model.size());

                            break;
                        default:
                            System.err.println("Unknown file extension.");
                            break;
                    }

                }
            }
        });
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selected = planList.getSelectedIndices();
                for(int i = selected.length - 1; i >= 0; i--)
                    ((DefaultListModel) planList.getModel()).remove(selected[i]);
                entryCount.setText("Entries: " + ((DefaultListModel) planList.getModel()).size());
            }
        });
        clearAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(int i = planList.getModel().getSize() - 1; i >= 0; i--)
                    ((DefaultListModel) planList.getModel()).remove(i);
                entryCount.setText("Entries: " + ((DefaultListModel) planList.getModel()).size());
            }
        });
        addPlanDescriptorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addDescriptor newDescriptor = new addDescriptor();
            }
        });
        bubblebombCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {

                maxColumn.setEnabled(!bubblebombCheckBox.isSelected());

            }
        });
        generatePLANButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                RPlan plan = new RPlan();

                Thing[] things = new Thing[planList.getModel().getSize() + 1];

                int UID = 1;

                Thing groupThing = null;

                for(int s = 0; s < planList.getModel().getSize(); s++)
                {
                    String descriptor = planList.getModel().getElementAt(s);

                    if(descriptor.contains(":"))
                    {
                        String guid = descriptor.split(" : ")[0];
                        String hash = descriptor.split(" : ")[1];

                        descriptor = preferHashCheckBox.isSelected() ? hash : guid;
                    }

                    Thing bubbleThing = new Thing(UID);
                    UID++;
                    bubbleThing.planGUID = new GUID(31743);

                    if(groupThing == null)
                    {
                        groupThing = new Thing(UID);
                        UID++;
                        groupThing.setPart(Part.REF, new PRef());
                        PGroup grp = new PGroup();
                        grp.flags = 2;
                        groupThing.setPart(Part.GROUP, grp);
                    }
                    bubbleThing.parent = groupThing;
                    bubbleThing.setPart(Part.BODY, new PBody());

                    PRenderMesh mesh = new PRenderMesh(new ResourceDescriptor(21180l, ResourceType.MESH));
                    mesh.castShadows = ShadowType.IF_ON_SCREEN;
                    mesh.animLoop = true;
                    mesh.RTTEnable = false;
                    mesh.visibilityFlags = 3;
                    mesh.boneThings = new Thing[]{bubbleThing};

                    bubbleThing.setPart(Part.RENDER_MESH, mesh);

                    PPos pos = new PPos(bubbleThing, 0);
                    if(!bubblebombCheckBox.isSelected())
                    {

                        int x = 0;
                        int y = s + 1;
                        int max = (Integer) maxColumn.getValue();

                        if (max < 1)
                            max = 1;

                        while(y > max)
                        {
                            x++;
                            y -= max;
                        }

                        pos.worldPosition.translate(210.0414f * x, 210.0414f * y, 0);
                    }
                    bubbleThing.setPart(Part.POS, pos);

                    PTrigger trigger = new PTrigger();
                    trigger.triggerType = TriggerType.RADIUS;
                    trigger.radiusMultiplier = 136.0f;
                    trigger.allZLayers = false;
                    trigger.hysteresisMultiplier = 1.0f;
                    trigger.enabled = true;

                    bubbleThing.setPart(Part.TRIGGER, trigger);

                    PScript script = new PScript();
                    script.instance.script = new ResourceDescriptor(27432l, ResourceType.SCRIPT);
                    script.instance.instanceLayout = new InstanceLayout();
                    script.instance.instanceLayout.instanceSize = 176;
                    ArrayList<FieldLayoutDetails> fields = new ArrayList<>();

                    FieldLayoutDetails deets0 = new FieldLayoutDetails();
                    deets0.arrayBaseMachineType = MachineType.VOID;
                    deets0.dimensionCount = 0;
                    deets0.fishType = BuiltinType.F32;
                    deets0.instanceOffset = 0;
                    deets0.machineType = MachineType.F32;
                    EnumSet<ModifierType> mods0 = EnumSet.noneOf(ModifierType.class);
                    mods0.add(ModifierType.PROTECTED);
                    mods0.add(ModifierType.DIVERGENT);
                    deets0.modifiers = mods0;
                    deets0.name = "CurRadiusAlpha";
                    deets0.type = ScriptObjectType.NULL;
                    deets0.value = null;
                    fields.add(deets0);

                    FieldLayoutDetails deets1 = new FieldLayoutDetails();
                    deets1.arrayBaseMachineType = MachineType.VOID;
                    deets1.dimensionCount = 0;
                    deets1.fishType = BuiltinType.F32;
                    deets1.instanceOffset = 4;
                    deets1.machineType = MachineType.F32;
                    EnumSet<ModifierType> mods1 = EnumSet.noneOf(ModifierType.class);
                    mods1.add(ModifierType.PROTECTED);
                    mods1.add(ModifierType.DIVERGENT);
                    deets1.modifiers = mods1;
                    deets1.name = "DstRadiusAlpha";
                    deets1.type = ScriptObjectType.NULL;
                    deets1.value = null;
                    fields.add(deets1);

                    FieldLayoutDetails deets2 = new FieldLayoutDetails();
                    deets2.arrayBaseMachineType = MachineType.VOID;
                    deets2.dimensionCount = 0;
                    deets2.fishType = BuiltinType.S32;
                    deets2.instanceOffset = 8;
                    deets2.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods2 = EnumSet.noneOf(ModifierType.class);
                    mods2.add(ModifierType.PROTECTED);
                    mods2.add(ModifierType.DIVERGENT);
                    deets2.modifiers = mods2;
                    deets2.name = "NeedUpdate";
                    deets2.type = ScriptObjectType.NULL;
                    deets2.value = null;
                    fields.add(deets2);

                    FieldLayoutDetails deets3 = new FieldLayoutDetails();
                    deets3.arrayBaseMachineType = MachineType.VOID;
                    deets3.dimensionCount = 0;
                    deets3.fishType = BuiltinType.S32;
                    deets3.instanceOffset = 12;
                    deets3.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods3 = EnumSet.noneOf(ModifierType.class);
                    mods3.add(ModifierType.PROTECTED);
                    deets3.modifiers = mods3;
                    deets3.name = "TweakPlayerNumber";
                    deets3.type = ScriptObjectType.NULL;
                    deets3.value = 1;
                    fields.add(deets3);

                    FieldLayoutDetails deets4 = new FieldLayoutDetails();
                    deets4.arrayBaseMachineType = MachineType.VOID;
                    deets4.dimensionCount = 0;
                    deets4.fishType = BuiltinType.S32;
                    deets4.instanceOffset = 16;
                    deets4.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods4 = EnumSet.noneOf(ModifierType.class);
                    mods3.add(ModifierType.PROTECTED);
                    deets4.modifiers = mods4;
                    deets4.name = "SliderFont";
                    deets4.type = ScriptObjectType.NULL;
                    deets4.value = 5;
                    fields.add(deets4);

                    FieldLayoutDetails deets5 = new FieldLayoutDetails();
                    deets5.arrayBaseMachineType = MachineType.VOID;
                    deets5.dimensionCount = 0;
                    deets5.fishType = BuiltinType.BOOL;
                    deets5.instanceOffset = 20;
                    deets5.machineType = MachineType.BOOL;
                    EnumSet<ModifierType> mods5 = EnumSet.noneOf(ModifierType.class);
                    mods5.add(ModifierType.PROTECTED);
                    mods5.add(ModifierType.DIVERGENT);
                    deets5.modifiers = mods5;
                    deets5.name = "LastSliderIsHorizontal";
                    deets5.type = ScriptObjectType.NULL;
                    deets5.value = null;
                    fields.add(deets5);

                    FieldLayoutDetails deets6 = new FieldLayoutDetails();
                    deets6.arrayBaseMachineType = MachineType.VOID;
                    deets6.dimensionCount = 0;
                    deets6.fishType = BuiltinType.BOOL;
                    deets6.instanceOffset = 21;
                    deets6.machineType = MachineType.BOOL;
                    EnumSet<ModifierType> mods6 = EnumSet.noneOf(ModifierType.class);
                    mods6.add(ModifierType.PROTECTED);
                    mods6.add(ModifierType.DIVERGENT);
                    deets6.modifiers = mods6;
                    deets6.name = "LastSliderHasFocus";
                    deets6.type = ScriptObjectType.NULL;
                    deets6.value = null;
                    fields.add(deets6);

                    FieldLayoutDetails deets7 = new FieldLayoutDetails();
                    deets7.arrayBaseMachineType = MachineType.VOID;
                    deets7.dimensionCount = 0;
                    deets7.fishType = BuiltinType.S32;
                    deets7.instanceOffset = 24;
                    deets7.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods7 = EnumSet.noneOf(ModifierType.class);
                    mods7.add(ModifierType.PROTECTED);
                    mods7.add(ModifierType.DIVERGENT);
                    deets7.modifiers = mods7;
                    deets7.name = "LastSliderInput";
                    deets7.type = ScriptObjectType.NULL;
                    deets7.value = null;
                    fields.add(deets7);

                    FieldLayoutDetails deets8 = new FieldLayoutDetails();
                    deets8.arrayBaseMachineType = MachineType.VOID;
                    deets8.dimensionCount = 0;
                    deets8.fishType = BuiltinType.V4;
                    deets8.instanceOffset = 32;
                    deets8.machineType = MachineType.V4;
                    EnumSet<ModifierType> mods8 = EnumSet.noneOf(ModifierType.class);
                    mods8.add(ModifierType.PROTECTED);
                    mods8.add(ModifierType.DIVERGENT);
                    deets8.modifiers = mods8;
                    deets8.name = "FillColour";
                    deets8.type = ScriptObjectType.NULL;
                    deets8.value = null;
                    fields.add(deets8);

                    FieldLayoutDetails deets9 = new FieldLayoutDetails();
                    deets9.arrayBaseMachineType = MachineType.VOID;
                    deets9.dimensionCount = 0;
                    deets9.fishType = BuiltinType.BOOL;
                    deets9.instanceOffset = 48;
                    deets9.machineType = MachineType.BOOL;
                    EnumSet<ModifierType> mods9 = EnumSet.noneOf(ModifierType.class);
                    mods9.add(ModifierType.PRIVATE);
                    mods9.add(ModifierType.DIVERGENT);
                    deets9.modifiers = mods9;
                    deets9.name = "BigLeft";
                    deets9.type = ScriptObjectType.NULL;
                    deets9.value = null;
                    fields.add(deets9);

                    FieldLayoutDetails deets10 = new FieldLayoutDetails();
                    deets10.arrayBaseMachineType = MachineType.VOID;
                    deets10.dimensionCount = 0;
                    deets10.fishType = BuiltinType.BOOL;
                    deets10.instanceOffset = 49;
                    deets10.machineType = MachineType.BOOL;
                    EnumSet<ModifierType> mods10 = EnumSet.noneOf(ModifierType.class);
                    mods10.add(ModifierType.PRIVATE);
                    mods10.add(ModifierType.DIVERGENT);
                    deets10.modifiers = mods10;
                    deets10.name = "BigRight";
                    deets10.type = ScriptObjectType.NULL;
                    deets10.value = null;
                    fields.add(deets10);

                    FieldLayoutDetails deets11 = new FieldLayoutDetails();
                    deets11.arrayBaseMachineType = MachineType.VOID;
                    deets11.dimensionCount = 0;
                    deets11.fishType = BuiltinType.S32;
                    deets11.instanceOffset = 52;
                    deets11.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods11 = EnumSet.noneOf(ModifierType.class);
                    mods11.add(ModifierType.PROTECTED);
                    deets11.modifiers = mods11;
                    deets11.name = "Direction";
                    deets11.type = ScriptObjectType.NULL;
                    deets11.value = 1;
                    fields.add(deets11);

                    FieldLayoutDetails deets12 = new FieldLayoutDetails();
                    deets12.arrayBaseMachineType = MachineType.VOID;
                    deets12.dimensionCount = 0;
                    deets12.fishType = BuiltinType.V4;
                    deets12.instanceOffset = 64;
                    deets12.machineType = MachineType.V4;
                    EnumSet<ModifierType> mods12 = EnumSet.noneOf(ModifierType.class);
                    mods12.add(ModifierType.PRIVATE);
                    mods12.add(ModifierType.DIVERGENT);
                    deets12.modifiers = mods12;
                    deets12.name = "DirectionUV";
                    deets12.type = ScriptObjectType.NULL;
                    deets12.value = null;
                    fields.add(deets12);

                    FieldLayoutDetails deets13 = new FieldLayoutDetails();
                    deets13.arrayBaseMachineType = MachineType.VOID;
                    deets13.dimensionCount = 0;
                    deets13.fishType = BuiltinType.V2;
                    deets13.instanceOffset = 80;
                    deets13.machineType = MachineType.V4;
                    EnumSet<ModifierType> mods13 = EnumSet.noneOf(ModifierType.class);
                    mods13.add(ModifierType.PRIVATE);
                    mods13.add(ModifierType.DIVERGENT);
                    deets13.modifiers = mods13;
                    deets13.name = "SideItemSize";
                    deets13.type = ScriptObjectType.NULL;
                    deets13.value = null;
                    fields.add(deets13);

                    FieldLayoutDetails deets14 = new FieldLayoutDetails();
                    deets14.arrayBaseMachineType = MachineType.VOID;
                    deets14.dimensionCount = 0;
                    deets14.fishType = BuiltinType.V2;
                    deets14.instanceOffset = 96;
                    deets14.machineType = MachineType.V4;
                    EnumSet<ModifierType> mods14 = EnumSet.noneOf(ModifierType.class);
                    mods14.add(ModifierType.PRIVATE);
                    mods14.add(ModifierType.DIVERGENT);
                    deets14.modifiers = mods14;
                    deets14.name = "LeftButtonSize";
                    deets14.type = ScriptObjectType.NULL;
                    deets14.value = null;
                    fields.add(deets14);

                    FieldLayoutDetails deets15 = new FieldLayoutDetails();
                    deets15.arrayBaseMachineType = MachineType.VOID;
                    deets15.dimensionCount = 0;
                    deets15.fishType = BuiltinType.V2;
                    deets15.instanceOffset = 112;
                    deets15.machineType = MachineType.V4;
                    EnumSet<ModifierType> mods15 = EnumSet.noneOf(ModifierType.class);
                    mods15.add(ModifierType.PRIVATE);
                    mods15.add(ModifierType.DIVERGENT);
                    deets15.modifiers = mods15;
                    deets15.name = "RightButtonSize";
                    deets15.type = ScriptObjectType.NULL;
                    deets15.value = null;
                    fields.add(deets15);

                    FieldLayoutDetails deets16 = new FieldLayoutDetails();
                    deets16.arrayBaseMachineType = MachineType.VOID;
                    deets16.dimensionCount = 0;
                    deets16.fishType = BuiltinType.BOOL;
                    deets16.instanceOffset = 128;
                    deets16.machineType = MachineType.BOOL;
                    EnumSet<ModifierType> mods16 = EnumSet.noneOf(ModifierType.class);
                    mods16.add(ModifierType.PROTECTED);
                    mods16.add(ModifierType.DIVERGENT);
                    deets16.modifiers = mods16;
                    deets16.name = "IsTweaking";
                    deets16.type = ScriptObjectType.NULL;
                    deets16.value = null;
                    fields.add(deets16);

                    FieldLayoutDetails deets17 = new FieldLayoutDetails();
                    deets17.arrayBaseMachineType = MachineType.VOID;
                    deets17.dimensionCount = 0;
                    deets17.fishType = BuiltinType.V4;
                    deets17.instanceOffset = 144;
                    deets17.machineType = MachineType.V4;
                    EnumSet<ModifierType> mods17 = EnumSet.noneOf(ModifierType.class);
                    mods17.add(ModifierType.PROTECTED);
                    deets17.modifiers = mods17;
                    deets17.name = "MainColour";
                    deets17.type = ScriptObjectType.NULL;
                    deets17.value = new Vector4f(0f, 0f, 0f, 1.0f);
                    fields.add(deets17);

                    FieldLayoutDetails deets18 = new FieldLayoutDetails();
                    deets18.arrayBaseMachineType = MachineType.VOID;
                    deets18.dimensionCount = 0;
                    deets18.fishType = BuiltinType.VOID;
                    deets18.instanceOffset = 160;
                    deets18.machineType = MachineType.OBJECT_REF;
                    EnumSet<ModifierType> mods18 = EnumSet.noneOf(ModifierType.class);
                    mods18.add(ModifierType.PROTECTED);
                    mods18.add(ModifierType.DIVERGENT);
                    deets18.modifiers = mods18;
                    deets18.name = "TweakTexture";
                    deets18.type = ScriptObjectType.NULL;
                    deets18.value = null;
                    fields.add(deets18);

                    FieldLayoutDetails deets19 = new FieldLayoutDetails();
                    deets19.arrayBaseMachineType = MachineType.VOID;
                    deets19.dimensionCount = 0;
                    deets19.fishType = BuiltinType.VOID;
                    deets19.instanceOffset = 164;
                    deets19.machineType = MachineType.OBJECT_REF;
                    EnumSet<ModifierType> mods19 = EnumSet.noneOf(ModifierType.class);
                    mods19.add(ModifierType.PRIVATE);
                    mods19.add(ModifierType.DIVERGENT);
                    deets19.modifiers = mods19;
                    deets19.name = "ContentsIcon";
                    deets19.type = ScriptObjectType.NULL;
                    deets19.value = null;
                    fields.add(deets19);

                    FieldLayoutDetails deets20 = new FieldLayoutDetails();
                    deets20.arrayBaseMachineType = MachineType.VOID;
                    deets20.dimensionCount = 0;
                    deets20.fishType = BuiltinType.S32;
                    deets20.instanceOffset = 168;
                    deets20.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods20 = EnumSet.noneOf(ModifierType.class);
                    mods20.add(ModifierType.PRIVATE);
                    mods20.add(ModifierType.DIVERGENT);
                    deets20.modifiers = mods20;
                    deets20.name = "CacheID";
                    deets20.type = ScriptObjectType.NULL;
                    deets20.value = null;
                    fields.add(deets20);

                    FieldLayoutDetails deets21 = new FieldLayoutDetails();
                    deets21.arrayBaseMachineType = MachineType.VOID;
                    deets21.dimensionCount = 0;
                    deets21.fishType = BuiltinType.S32;
                    deets21.instanceOffset = 172;
                    deets21.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods21 = EnumSet.noneOf(ModifierType.class);
                    mods21.add(ModifierType.PRIVATE);
                    mods21.add(ModifierType.DIVERGENT);
                    deets21.modifiers = mods21;
                    deets21.name = "ShareableIndex";
                    deets21.type = ScriptObjectType.NULL;
                    deets21.value = null;
                    fields.add(deets21);

                    script.instance.instanceLayout.fields = fields;

                    bubbleThing.setPart(Part.SCRIPT, script);

                    PShape shape = new PShape();
                    shape.polygon = new Polygon();
                    shape.polygon.vertices = new Vector3f[]{
                            new Vector3f(
                                    101.42221f,
                                    -27.175983f,
                                    -7.898791E-8f
                            ),
                            new Vector3f(
                                    90.93268f,
                                    -52.499985f,
                                    -7.898802E-8f
                            ),
                            new Vector3f(
                                    74.24622f,
                                    -74.2462f,
                                    -7.898757E-8f
                            ),
                            new Vector3f(
                                    52.500015f,
                                    -90.932655f,
                                    -7.898825E-8f
                            ),
                            new Vector3f(
                                    27.176016f,
                                    -101.42221f,
                                    -7.898825E-8f
                            ),
                            new Vector3f(
                                    1.3769088E-5f,
                                    -105.0f,
                                    -7.8987796E-8f
                            ),
                            new Vector3f(
                                    -27.175987f,
                                    -101.42221f,
                                    -7.898825E-8f
                            ),
                            new Vector3f(
                                    -52.499992f,
                                    -90.93268f,
                                    -7.898734E-8f
                            ),
                            new Vector3f(
                                    -74.24621f,
                                    -74.246216f,
                                    -7.8987796E-8f
                            ),
                            new Vector3f(
                                    -90.932655f,
                                    -52.500004f,
                                    -7.898802E-8f
                            ),
                            new Vector3f(
                                    -101.42221f,
                                    -27.176008f,
                                    -7.898802E-8f
                            ),
                            new Vector3f(
                                    -105.0f,
                                    -9.179392E-6f,
                                    -7.8987945E-8f
                            ),
                            new Vector3f(
                                    -101.42221f,
                                    27.17599f,
                                    -7.898802E-8f
                            ),
                            new Vector3f(
                                    -90.93268f,
                                    52.499996f,
                                    -7.898802E-8f
                            ),
                            new Vector3f(
                                    -74.246216f,
                                    74.24621f,
                                    -7.8987796E-8f
                            ),
                            new Vector3f(
                                    -52.500004f,
                                    90.93266f,
                                    -7.8987796E-8f
                            ),
                            new Vector3f(
                                    -27.176006f,
                                    101.42221f,
                                    -7.8987796E-8f
                            ),
                            new Vector3f(
                                    -4.589696E-6f,
                                    105.0f,
                                    -7.898825E-8f
                            ),
                            new Vector3f(
                                    27.175997f,
                                    101.42221f,
                                    -7.8987796E-8f
                            ),
                            new Vector3f(
                                    52.499996f,
                                    90.93268f,
                                    -7.898825E-8f
                            ),
                            new Vector3f(
                                    74.24621f,
                                    74.24621f,
                                    -7.898757E-8f
                            ),
                            new Vector3f(
                                    90.93266f,
                                    52.5f,
                                    -7.898802E-8f
                            ),
                            new Vector3f(
                                    101.42221f,
                                    27.175999f,
                                    -7.898791E-8f
                            ),
                            new Vector3f(
                                    105.0f,
                                    0.0f,
                                    -7.8987945E-8f
                            )
                    };

                    shape.polygon.loops = new int[]{shape.polygon.vertices.length};
                    shape.material = new ResourceDescriptor(17661, ResourceType.MATERIAL);
                    shape.thickness = 70.0f;
                    shape.massDepth = 1.0f;
                    shape.color = -11711155;
                    shape.bevelSize = 10.0f;
                    shape.interactPlayMode = 0;
                    shape.interactEditMode = 1;
                    shape.lethalType = LethalType.NOT;
                    shape.soundEnumOverride = AudioMaterial.NONE;
                    shape.flags = 7;

                    bubbleThing.setPart(Part.SHAPE, shape);

                    PRef ref = new PRef();
                    ref.plan = new ResourceDescriptor(descriptor, ResourceType.PLAN);
                    ref.childrenSelectable = true;
                    ref.stripChildren = false;

                    bubbleThing.setPart(Part.REF, ref);

                    PGameplayData gpData = new PGameplayData();
                    gpData.eggLink = new EggLink();
                    gpData.eggLink.plan = new ResourceDescriptor(descriptor, ResourceType.PLAN);
                    gpData.eggLink.shareable = shareableCheckBox.isSelected();
                    gpData.keyLink = null;

                    bubbleThing.setPart(Part.GAMEPLAY_DATA, gpData);
                    PGroup grup = new PGroup();
                    grup.planDescriptor = new ResourceDescriptor(31743l, ResourceType.PLAN);
                    bubbleThing.setPart(Part.GROUP, grup);

                    things[s] = bubbleThing;
                }

                things[things.length - 1] = groupThing;

                status = new status();
                status.amount.setText(Integer.toString(things.length - 1));

                Thread thread = new Thread() {
                    public void run() {
                        try {
                            plan.setThings(things);
                            status.close();
                            status = null;

                            plan.inventoryData.icon = new ResourceDescriptor(39493l, ResourceType.TEXTURE);
                            plan.inventoryData.userCreatedDetails = new UserCreatedDetails(bubblebombCheckBox.isSelected() ? "Bubblebomb" : "Bubblecolumn",
                                    "Contains " + planList.getModel().getSize() + " bubbles.");
                            plan.inventoryData.type.add(InventoryObjectType.USER_OBJECT);

                            byte[] compressed = Resource.compress(plan.build());

                            File file = FileChooser.openFile(bubblebombCheckBox.isSelected() ? "Bubblebomb" : "Bubblecolumn", "plan", true, false)[0];
                            try {
                                Files.write(file.toPath(), compressed);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }

                            this.stop();
                        } catch(Exception v) {
                            v.printStackTrace();
                            this.stop();
                        }
                    }
                };

                thread.start();
            }
        });
        generateBubbleBINButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                RLevel level = new RLevel();
                level.world.setPart(Part.BODY, new PBody());
                level.world.setPart(Part.POS, new PPos());
                PScript scriptLevel = new PScript();
                scriptLevel.instance.script = new ResourceDescriptor(19744l, ResourceType.SCRIPT);
                scriptLevel.instance.instanceLayout = new InstanceLayout();
                scriptLevel.instance.instanceLayout.instanceSize = 45;

                FieldLayoutDetails det1 = new FieldLayoutDetails();
                det1.name = "PlayerIndicatorList";
                det1.modifiers.add(ModifierType.PRIVATE);
                det1.modifiers.add(ModifierType.DIVERGENT);
                det1.machineType = MachineType.OBJECT_REF;
                det1.fishType = BuiltinType.VOID;
                det1.arrayBaseMachineType = MachineType.OBJECT_REF;
                det1.instanceOffset = 0;
                scriptLevel.instance.instanceLayout.fields.add(det1);

                FieldLayoutDetails det2 = new FieldLayoutDetails();
                det2.name = "DisplayNameFrame";
                det2.modifiers.add(ModifierType.PRIVATE);
                det2.modifiers.add(ModifierType.DIVERGENT);
                det2.machineType = MachineType.S32;
                det2.fishType = BuiltinType.S32;
                det2.arrayBaseMachineType = MachineType.VOID;
                det2.instanceOffset = 4;
                scriptLevel.instance.instanceLayout.fields.add(det2);

                FieldLayoutDetails det3 = new FieldLayoutDetails();
                det3.name = "DisplayNameTimer";
                det3.modifiers.add(ModifierType.PRIVATE);
                det3.modifiers.add(ModifierType.DIVERGENT);
                det3.machineType = MachineType.S32;
                det3.fishType = BuiltinType.S32;
                det3.arrayBaseMachineType = MachineType.VOID;
                det3.instanceOffset = 8;
                scriptLevel.instance.instanceLayout.fields.add(det3);

                FieldLayoutDetails det4 = new FieldLayoutDetails();
                det4.name = "OmnesBlack";
                det4.modifiers.add(ModifierType.PRIVATE);
                det4.modifiers.add(ModifierType.DIVERGENT);
                det4.machineType = MachineType.OBJECT_REF;
                det4.fishType = BuiltinType.VOID;
                det4.arrayBaseMachineType = MachineType.VOID;
                det4.instanceOffset = 12;
                scriptLevel.instance.instanceLayout.fields.add(det4);

                FieldLayoutDetails det5 = new FieldLayoutDetails();
                det5.name = "CurrentChallenge";
                det5.modifiers.add(ModifierType.PRIVATE);
                det5.machineType = MachineType.SAFE_PTR;
                det5.fishType = BuiltinType.VOID;
                det5.arrayBaseMachineType = MachineType.VOID;
                det5.instanceOffset = 16;
                scriptLevel.instance.instanceLayout.fields.add(det5);

                FieldLayoutDetails det6 = new FieldLayoutDetails();
                det6.name = "ScoreMultiplier";
                det6.modifiers.add(ModifierType.PRIVATE);
                det6.machineType = MachineType.S32;
                det6.fishType = BuiltinType.S32;
                det6.arrayBaseMachineType = MachineType.VOID;
                det6.instanceOffset = 20;
                det6.value = 0;
                scriptLevel.instance.instanceLayout.fields.add(det6);

                FieldLayoutDetails det7 = new FieldLayoutDetails();
                det7.name = "LastScoreTimer";
                det7.modifiers.add(ModifierType.PRIVATE);
                det7.machineType = MachineType.S32;
                det7.fishType = BuiltinType.S32;
                det7.arrayBaseMachineType = MachineType.VOID;
                det7.instanceOffset = 24;
                det7.value = 0;
                scriptLevel.instance.instanceLayout.fields.add(det7);

                FieldLayoutDetails det8 = new FieldLayoutDetails();
                det8.name = "LastScoreFrame";
                det8.modifiers.add(ModifierType.PRIVATE);
                det8.machineType = MachineType.S32;
                det8.fishType = BuiltinType.S32;
                det8.arrayBaseMachineType = MachineType.VOID;
                det8.instanceOffset = 28;
                det8.value = -1;
                scriptLevel.instance.instanceLayout.fields.add(det8);

                FieldLayoutDetails det9 = new FieldLayoutDetails();
                det9.name = "Bonuses";
                det9.modifiers.add(ModifierType.PRIVATE);
                det9.modifiers.add(ModifierType.DIVERGENT);
                det9.machineType = MachineType.OBJECT_REF;
                det9.fishType = BuiltinType.VOID;
                det9.arrayBaseMachineType = MachineType.OBJECT_REF;
                det9.instanceOffset = 32;
                scriptLevel.instance.instanceLayout.fields.add(det9);

                FieldLayoutDetails det10 = new FieldLayoutDetails();
                det10.name = "VCR";
                det10.modifiers.add(ModifierType.PRIVATE);
                det10.machineType = MachineType.OBJECT_REF;
                det10.fishType = BuiltinType.VOID;
                det10.arrayBaseMachineType = MachineType.VOID;
                det10.instanceOffset = 36;
                det10.type = ScriptObjectType.NULL;
                det10.value = new ScriptObject();
                ((ScriptObject)det10.value).type = ScriptObjectType.NULL;
                ((ScriptObject)det10.value).value = new ScriptInstance();
                ((ScriptInstance)((ScriptObject)det10.value).value).script = new ResourceDescriptor(60292l, ResourceType.SCRIPT);
                scriptLevel.instance.instanceLayout.fields.add(det10);

                FieldLayoutDetails det11 = new FieldLayoutDetails();
                det11.name = "ActiveDirectControlPromptList";
                det11.modifiers.add(ModifierType.PRIVATE);
                det11.machineType = MachineType.OBJECT_REF;
                det11.fishType = BuiltinType.VOID;
                det11.arrayBaseMachineType = MachineType.SAFE_PTR;
                det11.instanceOffset = 40;
                det11.type = ScriptObjectType.NULL;
                det11.value = new ScriptObject();
                ((ScriptObject)det11.value).type = ScriptObjectType.NULL;
                ((ScriptObject)det11.value).value = new FieldLayoutDetails();
                ((FieldLayoutDetails)((ScriptObject)det11.value).value).value = new ArrayList<>();
                scriptLevel.instance.instanceLayout.fields.add(det11);

                FieldLayoutDetails det12 = new FieldLayoutDetails();
                det12.name = "Finish";
                det12.modifiers.add(ModifierType.PRIVATE);
                det12.machineType = MachineType.BOOL;
                det12.fishType = BuiltinType.BOOL;
                det12.arrayBaseMachineType = MachineType.VOID;
                det12.instanceOffset = 44;
                det12.value = false;
                scriptLevel.instance.instanceLayout.fields.add(det12);

                PEffector effector = new PEffector();
                effector.viscosity = 0;
                effector.density = 0;
                effector.gravity = new Vector3f(0f, -2.7f, 0f);
                effector.pushBack = false;
                effector.swimmable = false;
                effector.viscosityCheap = 0.05f;
                effector.modScale = 1;

                level.world.setPart(Part.SCRIPT, scriptLevel);
                level.world.setPart(Part.EFFECTOR, effector);
                level.world.setPart(Part.METADATA, new PMetadata());
                level.world.setPart(Part.GAMEPLAY_DATA, new PGameplayData());
                level.playerRecord = new PlayerRecord();

                ArrayList<Thing> things = new ArrayList<>();

                int UID = 1;

                Thing LevelSettings = new Thing(UID);
                LevelSettings.planGUID = new GUID(31968l);
                LevelSettings.setPart(Part.POS, new PPos());

                LevelSettings preset1 = new LevelSettings();
                preset1.sunPosition = new Vector3f(-0.08494846f, 0.15752576f, 0.17f);
                preset1.sunPositionScale = 310309.28f;
                preset1.sunColor = new Vector4f(0.9131379f, 0.8234369f, 0.833018f, 1.0426357f);
                preset1.ambientColor = new Vector4f(0.46282214f, 0.49650395f, 0.55252314f, 1.0f);
                preset1.sunMultiplier = 1.4f;
                preset1.exposure = 1.0f;
                preset1.fogColor = new Vector4f(-0.015808199f, -0.010981388f, 0.003017962f, 1.0f);
                preset1.fogNear = 200;
                preset1.fogFar = 15000;
                preset1.rimColor = new Vector4f(0.8252604f, 0.69928366f, 0.55837226f, 1.5f);
                preset1.rimColor2 = new Vector4f(0.20606448f, 0.22871739f, 0.3489216f, 1.0f);

                LevelSettings preset2 = new LevelSettings();
                preset2.sunPosition = new Vector3f(0.08f, 0.24439862f, 0.17429554f);
                preset2.sunPositionScale = 310309.28f;
                preset2.sunColor = new Vector4f(0.9131379f, 0.8234369f, 0.833018f, 1.0426357f);
                preset2.ambientColor = new Vector4f(0.80777466f, 0.8210661f, 0.7313651f, 1.0f);
                preset2.sunMultiplier = 1.5f;
                preset2.exposure = 1.05f;
                preset2.fogColor = new Vector4f(0.6026691f, 0.76635367f, 0.78096366f, 1.0f);
                preset2.fogNear = 200;
                preset2.fogFar = 15000;
                preset2.rimColor = new Vector4f(0.8252604f, 0.69928366f, 0.55837226f, 1.5f);
                preset2.rimColor2 = new Vector4f(0.35081163f, 0.6995919f, 0.9100586f, 1.0f);

                PLevelSettings levSet = new PLevelSettings();
                levSet.presets = new ArrayList<>();
                levSet.presets.add(preset1);
                levSet.presets.add(preset2);
                levSet.backdropAmbience = "ambiences/amb_empty_world";
                levSet.sunPosition = new Vector3f(0.08f, 0.24439862f, 0.17429554f);
                levSet.sunPositionScale = 310309.28f;
                levSet.sunColor = new Vector4f(0.9131379f, 0.8234369f, 0.833018f, 1.0426357f);
                levSet.ambientColor = new Vector4f(0.80777466f, 0.8210661f, 0.7313651f, 1.0f);
                levSet.sunMultiplier = 1.5f;
                levSet.exposure = 1.05f;
                levSet.fogColor = new Vector4f(0.6026691f, 0.76635367f, 0.78096366f, 1.0f);
                levSet.fogNear = 200;
                levSet.fogFar = 15000;
                levSet.rimColor = new Vector4f(0.8252604f, 0.69928366f, 0.55837226f, 1.5f);
                levSet.rimColor2 = new Vector4f(0.35081163f, 0.6995919f, 0.9100586f, 1.0f);

                LevelSettings.setPart(Part.LEVEL_SETTINGS, levSet);

                PRef ref = new PRef();
                ref.plan = new ResourceDescriptor(31968l, ResourceType.PLAN);
                ref.oldLifetime = 0;
                ref.oldAliveFrames = 18;
                ref.childrenSelectable = false;
                ref.stripChildren = true;

                LevelSettings.setPart(Part.REF, ref);

                UID++;
                things.add(LevelSettings);

                Thing border1 = new Thing(UID);
                border1.setPart(Part.BODY, new PBody());

                PPos posB1 = new PPos();
                posB1.localPosition = new Matrix4f().identity()
                        .translate(-166641.97f, -14044.834f, 0.0f)
                        .rotate(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f))
                        .scale(9.781206f, 2.666278f, 1.0f);
                posB1.worldPosition = posB1.localPosition;
                posB1.animHash = 0;

                border1.setPart(Part.POS, posB1);

                PShape shapeB1 = new PShape();
                shapeB1.polygon = new Polygon();
                shapeB1.polygon.vertices = new Vector3f[]{
                        new Vector3f(14848.0f, 23296.0f, 0.0f),
                        new Vector3f(23744.197f, 23296.002f, 0.0f),
                        new Vector3f(23744.197f, 20992.0f, 0.0f),
                        new Vector3f(14848.0f, 20992.0f, 0.0f),
                };
                shapeB1.material = new ResourceDescriptor(5140l, ResourceType.MATERIAL);
                shapeB1.thickness = 500.0f;
                shapeB1.massDepth = 5.0f;
                shapeB1.color = -11711155;
                shapeB1.bevelSize = 10.0f;
                shapeB1.lethalType = LethalType.NOT;
                shapeB1.soundEnumOverride = AudioMaterial.NONE;
                shapeB1.flags = 7;
                shapeB1.polygon.loops = new int[]{4};

                border1.setPart(Part.SHAPE, shapeB1);

                PRef refB1 = new PRef();
                refB1.plan = new ResourceDescriptor(31781l, ResourceType.PLAN);
                refB1.oldLifetime = 0;
                refB1.oldAliveFrames = 18;
                refB1.childrenSelectable = true;
                refB1.stripChildren = false;

                border1.setPart(Part.REF, refB1);

                PGroup groupB1 = new PGroup();
                groupB1.planDescriptor = null;
                groupB1.creator = new NetworkPlayerID("");
                groupB1.emitter = null;
                groupB1.lifetime = 0;
                groupB1.aliveFrames = 357;
                groupB1.flags = 0;

                border1.setPart(Part.GROUP, groupB1);

                UID++;
                things.add(border1);

                Thing border2 = new Thing(UID);
                border2.setPart(Part.BODY, new PBody());
                border2.parent = border1;
                border2.groupHead = border1;

                PPos posB2 = new PPos();
                posB2.localPosition = new Matrix4f().identity()
                        .translate(21279.145f, -112229.234f, 0.0f)
                        .rotate(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f))
                        .scale(0.14560874f, 5.514637f, 1.0f);
                posB2.worldPosition = new Matrix4f().identity()
                        .translate(41493.727f, -313279.16f, 0.0f)
                        .rotate(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f))
                        .scale(1.424229f, 14.703554f, 1.0f);
                posB2.animHash = 0;

                border2.setPart(Part.POS, posB2);

                PShape shapeB2 = new PShape();
                shapeB2.polygon = new Polygon();
                shapeB2.polygon.vertices = new Vector3f[]{
                        new Vector3f(14848.001f, 24403.418f, 0.0f),
                        new Vector3f(19200.0f, 24403.418f, 0.0f),
                        new Vector3f(19200.0f, 20992.0f, 0.0f),
                        new Vector3f(14848.0f, 20992.0f, 0.0f),
                };
                shapeB2.material = new ResourceDescriptor(5140l, ResourceType.MATERIAL);
                shapeB2.thickness = 500.0f;
                shapeB2.massDepth = 5.0f;
                shapeB2.color = -11711155;
                shapeB2.bevelSize = 10.0f;
                shapeB2.lethalType = LethalType.NOT;
                shapeB2.soundEnumOverride = AudioMaterial.NONE;
                shapeB2.flags = 7;
                shapeB2.polygon.loops = new int[]{4};

                border2.setPart(Part.SHAPE, shapeB2);

                UID++;
                things.add(border2);

                Thing border3 = new Thing(UID);
                border3.setPart(Part.BODY, new PBody());
                border3.parent = border1;
                border3.groupHead = border1;

                PPos posB3 = new PPos();
                posB3.localPosition = new Matrix4f().identity()
                        .translate(12196.039f, -112229.234f, 0.0f)
                        .rotate(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f))
                        .scale(0.14560874f, 5.514637f, 1.0f);
                posB3.worldPosition = new Matrix4f().identity()
                        .translate(-47349.992f, -313279.16f, 0.0f)
                        .rotate(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f))
                        .scale(1.424229f, 14.703554f, 1.0f);
                posB3.animHash = 0;

                border3.setPart(Part.POS, posB3);

                PShape shapeB3 = new PShape();
                shapeB3.polygon = new Polygon();
                shapeB3.polygon.vertices = new Vector3f[]{
                        new Vector3f(14847.998f, 24403.418f, 0.0f),
                        new Vector3f(19200.0f, 24403.418f, 0.0f),
                        new Vector3f(19200.0f, 20992.0f, 0.0f),
                        new Vector3f(14848.0f, 20992.0f, 0.0f),
                };
                shapeB3.material = new ResourceDescriptor(5140l, ResourceType.MATERIAL);
                shapeB3.thickness = 500.0f;
                shapeB3.massDepth = 5.0f;
                shapeB3.color = -11711155;
                shapeB3.bevelSize = 10.0f;
                shapeB3.lethalType = LethalType.NOT;
                shapeB3.soundEnumOverride = AudioMaterial.NONE;
                shapeB3.flags = 7;
                shapeB3.polygon.loops = new int[]{4};

                border3.setPart(Part.SHAPE, shapeB3);

                UID++;
                things.add(border3);

                Thing border4 = new Thing(UID);
                border4.setPart(Part.BODY, new PBody());
                border4.parent = border1;
                border4.groupHead = border1;

                PPos posB4 = new PPos();
                posB4.localPosition = new Matrix4f().identity()
                        .translate(-15032.436f, -17407.568f, 0.0f)
                        .rotate(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f))
                        .scale(2.012423f, 1.0f, 1.0f);
                posB4.worldPosition = new Matrix4f().identity()
                        .translate(-313677.3f, -60458.246f, 0.0f)
                        .rotate(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f))
                        .scale(19.683924f, 2.666278f, 1.0f);
                posB4.animHash = 0;

                border4.setPart(Part.POS, posB4);

                PShape shapeB4 = new PShape();
                shapeB4.polygon = new Polygon();
                shapeB4.polygon.vertices = new Vector3f[]{
                        new Vector3f(14848.0f, 23296.0f, 0.0f),
                        new Vector3f(19200.0f, 23296.0f, 0.0f),
                        new Vector3f(19200.0f, 20992.0f, 0.0f),
                        new Vector3f(14848.0f, 20992.0f, 0.0f),
                };
                shapeB4.material = new ResourceDescriptor(5140l, ResourceType.MATERIAL);
                shapeB4.thickness = 500.0f;
                shapeB4.massDepth = 5.0f;
                shapeB4.color = -11711155;
                shapeB4.bevelSize = 10.0f;
                shapeB4.lethalType = LethalType.NOT;
                shapeB4.soundEnumOverride = AudioMaterial.NONE;
                shapeB4.flags = 7;
                shapeB4.polygon.loops = new int[]{4};

                border4.setPart(Part.SHAPE, shapeB4);

                UID++;
                things.add(border4);

                Thing groupThing = null;

                for(int s = 0; s < planList.getModel().getSize(); s++)
                {
                    String descriptor = planList.getModel().getElementAt(s);

                    if(descriptor.contains(":"))
                    {
                        String guid = descriptor.split(" : ")[0];
                        String hash = descriptor.split(" : ")[1];

                        descriptor = preferHashCheckBox.isSelected() ? hash : guid;
                    }

                    Thing bubbleThing = new Thing(UID);
                    UID++;
                    bubbleThing.planGUID = new GUID(31743);

                    if(groupThing == null)
                    {
                        groupThing = new Thing(UID);
                        UID++;
                        groupThing.setPart(Part.REF, new PRef());
                        PGroup grp = new PGroup();
                        grp.flags = 2;
                        groupThing.setPart(Part.GROUP, grp);
                    }
                    bubbleThing.parent = groupThing;
                    bubbleThing.setPart(Part.BODY, new PBody());

                    PRenderMesh mesh = new PRenderMesh(new ResourceDescriptor(21180l, ResourceType.MESH));
                    mesh.castShadows = ShadowType.IF_ON_SCREEN;
                    mesh.animLoop = true;
                    mesh.RTTEnable = false;
                    mesh.visibilityFlags = 3;
                    mesh.boneThings = new Thing[]{bubbleThing};

                    bubbleThing.setPart(Part.RENDER_MESH, mesh);

                    PPos pos = new PPos(bubbleThing, 0);

                    pos.worldPosition = new Matrix4f().identity()
                            .translate(-19223.87f, 1760.3801f, 4.813927E-13f)
                            .rotate(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f))
                            .scale(0.99999976f, 0.99999994f, 0.67164725f);
                    pos.localPosition = pos.worldPosition;

                    if(!bubblebombCheckBox.isSelected())
                    {

                        int x = 0;
                        int y = s + 1;
                        int max = (Integer) maxColumn.getValue();

                        if (max < 1)
                            max = 1;

                        while(y > max)
                        {
                            x++;
                            y -= max;
                        }

                        pos.worldPosition = new Matrix4f().identity()
                                .translate(-19223.87f + (210.0414f * x), 1760.3801f + (210.0414f * y), 4.813927E-13f)
                                .rotate(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f))
                                .scale(0.99999976f, 0.99999994f, 0.67164725f);
                        pos.localPosition = pos.worldPosition;

                    }
                    bubbleThing.setPart(Part.POS, pos);

                    PTrigger trigger = new PTrigger();
                    trigger.triggerType = TriggerType.RADIUS;
                    trigger.radiusMultiplier = 136.0f;
                    trigger.allZLayers = false;
                    trigger.hysteresisMultiplier = 1.0f;
                    trigger.enabled = true;

                    bubbleThing.setPart(Part.TRIGGER, trigger);

                    PScript script = new PScript();
                    script.instance.script = new ResourceDescriptor(27432l, ResourceType.SCRIPT);
                    script.instance.instanceLayout = new InstanceLayout();
                    script.instance.instanceLayout.instanceSize = 176;
                    ArrayList<FieldLayoutDetails> fields = new ArrayList<>();

                    FieldLayoutDetails deets0 = new FieldLayoutDetails();
                    deets0.arrayBaseMachineType = MachineType.VOID;
                    deets0.dimensionCount = 0;
                    deets0.fishType = BuiltinType.F32;
                    deets0.instanceOffset = 0;
                    deets0.machineType = MachineType.F32;
                    EnumSet<ModifierType> mods0 = EnumSet.noneOf(ModifierType.class);
                    mods0.add(ModifierType.PROTECTED);
                    mods0.add(ModifierType.DIVERGENT);
                    deets0.modifiers = mods0;
                    deets0.name = "CurRadiusAlpha";
                    deets0.type = ScriptObjectType.NULL;
                    deets0.value = null;
                    fields.add(deets0);

                    FieldLayoutDetails deets1 = new FieldLayoutDetails();
                    deets1.arrayBaseMachineType = MachineType.VOID;
                    deets1.dimensionCount = 0;
                    deets1.fishType = BuiltinType.F32;
                    deets1.instanceOffset = 4;
                    deets1.machineType = MachineType.F32;
                    EnumSet<ModifierType> mods1 = EnumSet.noneOf(ModifierType.class);
                    mods1.add(ModifierType.PROTECTED);
                    mods1.add(ModifierType.DIVERGENT);
                    deets1.modifiers = mods1;
                    deets1.name = "DstRadiusAlpha";
                    deets1.type = ScriptObjectType.NULL;
                    deets1.value = null;
                    fields.add(deets1);

                    FieldLayoutDetails deets2 = new FieldLayoutDetails();
                    deets2.arrayBaseMachineType = MachineType.VOID;
                    deets2.dimensionCount = 0;
                    deets2.fishType = BuiltinType.S32;
                    deets2.instanceOffset = 8;
                    deets2.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods2 = EnumSet.noneOf(ModifierType.class);
                    mods2.add(ModifierType.PROTECTED);
                    mods2.add(ModifierType.DIVERGENT);
                    deets2.modifiers = mods2;
                    deets2.name = "NeedUpdate";
                    deets2.type = ScriptObjectType.NULL;
                    deets2.value = null;
                    fields.add(deets2);

                    FieldLayoutDetails deets3 = new FieldLayoutDetails();
                    deets3.arrayBaseMachineType = MachineType.VOID;
                    deets3.dimensionCount = 0;
                    deets3.fishType = BuiltinType.S32;
                    deets3.instanceOffset = 12;
                    deets3.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods3 = EnumSet.noneOf(ModifierType.class);
                    mods3.add(ModifierType.PROTECTED);
                    deets3.modifiers = mods3;
                    deets3.name = "TweakPlayerNumber";
                    deets3.type = ScriptObjectType.NULL;
                    deets3.value = 1;
                    fields.add(deets3);

                    FieldLayoutDetails deets4 = new FieldLayoutDetails();
                    deets4.arrayBaseMachineType = MachineType.VOID;
                    deets4.dimensionCount = 0;
                    deets4.fishType = BuiltinType.S32;
                    deets4.instanceOffset = 16;
                    deets4.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods4 = EnumSet.noneOf(ModifierType.class);
                    mods3.add(ModifierType.PROTECTED);
                    deets4.modifiers = mods4;
                    deets4.name = "SliderFont";
                    deets4.type = ScriptObjectType.NULL;
                    deets4.value = 5;
                    fields.add(deets4);

                    FieldLayoutDetails deets5 = new FieldLayoutDetails();
                    deets5.arrayBaseMachineType = MachineType.VOID;
                    deets5.dimensionCount = 0;
                    deets5.fishType = BuiltinType.BOOL;
                    deets5.instanceOffset = 20;
                    deets5.machineType = MachineType.BOOL;
                    EnumSet<ModifierType> mods5 = EnumSet.noneOf(ModifierType.class);
                    mods5.add(ModifierType.PROTECTED);
                    mods5.add(ModifierType.DIVERGENT);
                    deets5.modifiers = mods5;
                    deets5.name = "LastSliderIsHorizontal";
                    deets5.type = ScriptObjectType.NULL;
                    deets5.value = null;
                    fields.add(deets5);

                    FieldLayoutDetails deets6 = new FieldLayoutDetails();
                    deets6.arrayBaseMachineType = MachineType.VOID;
                    deets6.dimensionCount = 0;
                    deets6.fishType = BuiltinType.BOOL;
                    deets6.instanceOffset = 21;
                    deets6.machineType = MachineType.BOOL;
                    EnumSet<ModifierType> mods6 = EnumSet.noneOf(ModifierType.class);
                    mods6.add(ModifierType.PROTECTED);
                    mods6.add(ModifierType.DIVERGENT);
                    deets6.modifiers = mods6;
                    deets6.name = "LastSliderHasFocus";
                    deets6.type = ScriptObjectType.NULL;
                    deets6.value = null;
                    fields.add(deets6);

                    FieldLayoutDetails deets7 = new FieldLayoutDetails();
                    deets7.arrayBaseMachineType = MachineType.VOID;
                    deets7.dimensionCount = 0;
                    deets7.fishType = BuiltinType.S32;
                    deets7.instanceOffset = 24;
                    deets7.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods7 = EnumSet.noneOf(ModifierType.class);
                    mods7.add(ModifierType.PROTECTED);
                    mods7.add(ModifierType.DIVERGENT);
                    deets7.modifiers = mods7;
                    deets7.name = "LastSliderInput";
                    deets7.type = ScriptObjectType.NULL;
                    deets7.value = null;
                    fields.add(deets7);

                    FieldLayoutDetails deets8 = new FieldLayoutDetails();
                    deets8.arrayBaseMachineType = MachineType.VOID;
                    deets8.dimensionCount = 0;
                    deets8.fishType = BuiltinType.V4;
                    deets8.instanceOffset = 32;
                    deets8.machineType = MachineType.V4;
                    EnumSet<ModifierType> mods8 = EnumSet.noneOf(ModifierType.class);
                    mods8.add(ModifierType.PROTECTED);
                    mods8.add(ModifierType.DIVERGENT);
                    deets8.modifiers = mods8;
                    deets8.name = "FillColour";
                    deets8.type = ScriptObjectType.NULL;
                    deets8.value = null;
                    fields.add(deets8);

                    FieldLayoutDetails deets9 = new FieldLayoutDetails();
                    deets9.arrayBaseMachineType = MachineType.VOID;
                    deets9.dimensionCount = 0;
                    deets9.fishType = BuiltinType.BOOL;
                    deets9.instanceOffset = 48;
                    deets9.machineType = MachineType.BOOL;
                    EnumSet<ModifierType> mods9 = EnumSet.noneOf(ModifierType.class);
                    mods9.add(ModifierType.PRIVATE);
                    mods9.add(ModifierType.DIVERGENT);
                    deets9.modifiers = mods9;
                    deets9.name = "BigLeft";
                    deets9.type = ScriptObjectType.NULL;
                    deets9.value = null;
                    fields.add(deets9);

                    FieldLayoutDetails deets10 = new FieldLayoutDetails();
                    deets10.arrayBaseMachineType = MachineType.VOID;
                    deets10.dimensionCount = 0;
                    deets10.fishType = BuiltinType.BOOL;
                    deets10.instanceOffset = 49;
                    deets10.machineType = MachineType.BOOL;
                    EnumSet<ModifierType> mods10 = EnumSet.noneOf(ModifierType.class);
                    mods10.add(ModifierType.PRIVATE);
                    mods10.add(ModifierType.DIVERGENT);
                    deets10.modifiers = mods10;
                    deets10.name = "BigRight";
                    deets10.type = ScriptObjectType.NULL;
                    deets10.value = null;
                    fields.add(deets10);

                    FieldLayoutDetails deets11 = new FieldLayoutDetails();
                    deets11.arrayBaseMachineType = MachineType.VOID;
                    deets11.dimensionCount = 0;
                    deets11.fishType = BuiltinType.S32;
                    deets11.instanceOffset = 52;
                    deets11.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods11 = EnumSet.noneOf(ModifierType.class);
                    mods11.add(ModifierType.PROTECTED);
                    deets11.modifiers = mods11;
                    deets11.name = "Direction";
                    deets11.type = ScriptObjectType.NULL;
                    deets11.value = 1;
                    fields.add(deets11);

                    FieldLayoutDetails deets12 = new FieldLayoutDetails();
                    deets12.arrayBaseMachineType = MachineType.VOID;
                    deets12.dimensionCount = 0;
                    deets12.fishType = BuiltinType.V4;
                    deets12.instanceOffset = 64;
                    deets12.machineType = MachineType.V4;
                    EnumSet<ModifierType> mods12 = EnumSet.noneOf(ModifierType.class);
                    mods12.add(ModifierType.PRIVATE);
                    mods12.add(ModifierType.DIVERGENT);
                    deets12.modifiers = mods12;
                    deets12.name = "DirectionUV";
                    deets12.type = ScriptObjectType.NULL;
                    deets12.value = null;
                    fields.add(deets12);

                    FieldLayoutDetails deets13 = new FieldLayoutDetails();
                    deets13.arrayBaseMachineType = MachineType.VOID;
                    deets13.dimensionCount = 0;
                    deets13.fishType = BuiltinType.V2;
                    deets13.instanceOffset = 80;
                    deets13.machineType = MachineType.V4;
                    EnumSet<ModifierType> mods13 = EnumSet.noneOf(ModifierType.class);
                    mods13.add(ModifierType.PRIVATE);
                    mods13.add(ModifierType.DIVERGENT);
                    deets13.modifiers = mods13;
                    deets13.name = "SideItemSize";
                    deets13.type = ScriptObjectType.NULL;
                    deets13.value = null;
                    fields.add(deets13);

                    FieldLayoutDetails deets14 = new FieldLayoutDetails();
                    deets14.arrayBaseMachineType = MachineType.VOID;
                    deets14.dimensionCount = 0;
                    deets14.fishType = BuiltinType.V2;
                    deets14.instanceOffset = 96;
                    deets14.machineType = MachineType.V4;
                    EnumSet<ModifierType> mods14 = EnumSet.noneOf(ModifierType.class);
                    mods14.add(ModifierType.PRIVATE);
                    mods14.add(ModifierType.DIVERGENT);
                    deets14.modifiers = mods14;
                    deets14.name = "LeftButtonSize";
                    deets14.type = ScriptObjectType.NULL;
                    deets14.value = null;
                    fields.add(deets14);

                    FieldLayoutDetails deets15 = new FieldLayoutDetails();
                    deets15.arrayBaseMachineType = MachineType.VOID;
                    deets15.dimensionCount = 0;
                    deets15.fishType = BuiltinType.V2;
                    deets15.instanceOffset = 112;
                    deets15.machineType = MachineType.V4;
                    EnumSet<ModifierType> mods15 = EnumSet.noneOf(ModifierType.class);
                    mods15.add(ModifierType.PRIVATE);
                    mods15.add(ModifierType.DIVERGENT);
                    deets15.modifiers = mods15;
                    deets15.name = "RightButtonSize";
                    deets15.type = ScriptObjectType.NULL;
                    deets15.value = null;
                    fields.add(deets15);

                    FieldLayoutDetails deets16 = new FieldLayoutDetails();
                    deets16.arrayBaseMachineType = MachineType.VOID;
                    deets16.dimensionCount = 0;
                    deets16.fishType = BuiltinType.BOOL;
                    deets16.instanceOffset = 128;
                    deets16.machineType = MachineType.BOOL;
                    EnumSet<ModifierType> mods16 = EnumSet.noneOf(ModifierType.class);
                    mods16.add(ModifierType.PROTECTED);
                    mods16.add(ModifierType.DIVERGENT);
                    deets16.modifiers = mods16;
                    deets16.name = "IsTweaking";
                    deets16.type = ScriptObjectType.NULL;
                    deets16.value = null;
                    fields.add(deets16);

                    FieldLayoutDetails deets17 = new FieldLayoutDetails();
                    deets17.arrayBaseMachineType = MachineType.VOID;
                    deets17.dimensionCount = 0;
                    deets17.fishType = BuiltinType.V4;
                    deets17.instanceOffset = 144;
                    deets17.machineType = MachineType.V4;
                    EnumSet<ModifierType> mods17 = EnumSet.noneOf(ModifierType.class);
                    mods17.add(ModifierType.PROTECTED);
                    deets17.modifiers = mods17;
                    deets17.name = "MainColour";
                    deets17.type = ScriptObjectType.NULL;
                    deets17.value = new Vector4f(0f, 0f, 0f, 1.0f);
                    fields.add(deets17);

                    FieldLayoutDetails deets18 = new FieldLayoutDetails();
                    deets18.arrayBaseMachineType = MachineType.VOID;
                    deets18.dimensionCount = 0;
                    deets18.fishType = BuiltinType.VOID;
                    deets18.instanceOffset = 160;
                    deets18.machineType = MachineType.OBJECT_REF;
                    EnumSet<ModifierType> mods18 = EnumSet.noneOf(ModifierType.class);
                    mods18.add(ModifierType.PROTECTED);
                    mods18.add(ModifierType.DIVERGENT);
                    deets18.modifiers = mods18;
                    deets18.name = "TweakTexture";
                    deets18.type = ScriptObjectType.NULL;
                    deets18.value = null;
                    fields.add(deets18);

                    FieldLayoutDetails deets19 = new FieldLayoutDetails();
                    deets19.arrayBaseMachineType = MachineType.VOID;
                    deets19.dimensionCount = 0;
                    deets19.fishType = BuiltinType.VOID;
                    deets19.instanceOffset = 164;
                    deets19.machineType = MachineType.OBJECT_REF;
                    EnumSet<ModifierType> mods19 = EnumSet.noneOf(ModifierType.class);
                    mods19.add(ModifierType.PRIVATE);
                    mods19.add(ModifierType.DIVERGENT);
                    deets19.modifiers = mods19;
                    deets19.name = "ContentsIcon";
                    deets19.type = ScriptObjectType.NULL;
                    deets19.value = null;
                    fields.add(deets19);

                    FieldLayoutDetails deets20 = new FieldLayoutDetails();
                    deets20.arrayBaseMachineType = MachineType.VOID;
                    deets20.dimensionCount = 0;
                    deets20.fishType = BuiltinType.S32;
                    deets20.instanceOffset = 168;
                    deets20.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods20 = EnumSet.noneOf(ModifierType.class);
                    mods20.add(ModifierType.PRIVATE);
                    mods20.add(ModifierType.DIVERGENT);
                    deets20.modifiers = mods20;
                    deets20.name = "CacheID";
                    deets20.type = ScriptObjectType.NULL;
                    deets20.value = null;
                    fields.add(deets20);

                    FieldLayoutDetails deets21 = new FieldLayoutDetails();
                    deets21.arrayBaseMachineType = MachineType.VOID;
                    deets21.dimensionCount = 0;
                    deets21.fishType = BuiltinType.S32;
                    deets21.instanceOffset = 172;
                    deets21.machineType = MachineType.S32;
                    EnumSet<ModifierType> mods21 = EnumSet.noneOf(ModifierType.class);
                    mods21.add(ModifierType.PRIVATE);
                    mods21.add(ModifierType.DIVERGENT);
                    deets21.modifiers = mods21;
                    deets21.name = "ShareableIndex";
                    deets21.type = ScriptObjectType.NULL;
                    deets21.value = null;
                    fields.add(deets21);

                    script.instance.instanceLayout.fields = fields;

                    bubbleThing.setPart(Part.SCRIPT, script);

                    PShape shape = new PShape();
                    shape.polygon = new Polygon();
                    shape.polygon.vertices = new Vector3f[]{
                            new Vector3f(
                                    101.42221f,
                                    -27.175983f,
                                    -7.898791E-8f
                            ),
                            new Vector3f(
                                    90.93268f,
                                    -52.499985f,
                                    -7.898802E-8f
                            ),
                            new Vector3f(
                                    74.24622f,
                                    -74.2462f,
                                    -7.898757E-8f
                            ),
                            new Vector3f(
                                    52.500015f,
                                    -90.932655f,
                                    -7.898825E-8f
                            ),
                            new Vector3f(
                                    27.176016f,
                                    -101.42221f,
                                    -7.898825E-8f
                            ),
                            new Vector3f(
                                    1.3769088E-5f,
                                    -105.0f,
                                    -7.8987796E-8f
                            ),
                            new Vector3f(
                                    -27.175987f,
                                    -101.42221f,
                                    -7.898825E-8f
                            ),
                            new Vector3f(
                                    -52.499992f,
                                    -90.93268f,
                                    -7.898734E-8f
                            ),
                            new Vector3f(
                                    -74.24621f,
                                    -74.246216f,
                                    -7.8987796E-8f
                            ),
                            new Vector3f(
                                    -90.932655f,
                                    -52.500004f,
                                    -7.898802E-8f
                            ),
                            new Vector3f(
                                    -101.42221f,
                                    -27.176008f,
                                    -7.898802E-8f
                            ),
                            new Vector3f(
                                    -105.0f,
                                    -9.179392E-6f,
                                    -7.8987945E-8f
                            ),
                            new Vector3f(
                                    -101.42221f,
                                    27.17599f,
                                    -7.898802E-8f
                            ),
                            new Vector3f(
                                    -90.93268f,
                                    52.499996f,
                                    -7.898802E-8f
                            ),
                            new Vector3f(
                                    -74.246216f,
                                    74.24621f,
                                    -7.8987796E-8f
                            ),
                            new Vector3f(
                                    -52.500004f,
                                    90.93266f,
                                    -7.8987796E-8f
                            ),
                            new Vector3f(
                                    -27.176006f,
                                    101.42221f,
                                    -7.8987796E-8f
                            ),
                            new Vector3f(
                                    -4.589696E-6f,
                                    105.0f,
                                    -7.898825E-8f
                            ),
                            new Vector3f(
                                    27.175997f,
                                    101.42221f,
                                    -7.8987796E-8f
                            ),
                            new Vector3f(
                                    52.499996f,
                                    90.93268f,
                                    -7.898825E-8f
                            ),
                            new Vector3f(
                                    74.24621f,
                                    74.24621f,
                                    -7.898757E-8f
                            ),
                            new Vector3f(
                                    90.93266f,
                                    52.5f,
                                    -7.898802E-8f
                            ),
                            new Vector3f(
                                    101.42221f,
                                    27.175999f,
                                    -7.898791E-8f
                            ),
                            new Vector3f(
                                    105.0f,
                                    0.0f,
                                    -7.8987945E-8f
                            )
                    };

                    shape.polygon.loops = new int[]{shape.polygon.vertices.length};
                    shape.material = new ResourceDescriptor(17661, ResourceType.MATERIAL);
                    shape.thickness = 70.0f;
                    shape.massDepth = 1.0f;
                    shape.color = -11711155;
                    shape.bevelSize = 10.0f;
                    shape.interactPlayMode = 0;
                    shape.interactEditMode = 1;
                    shape.lethalType = LethalType.NOT;
                    shape.soundEnumOverride = AudioMaterial.NONE;
                    shape.flags = 7;

                    bubbleThing.setPart(Part.SHAPE, shape);

                    PRef refBubble = new PRef();
                    refBubble.plan = new ResourceDescriptor(descriptor, ResourceType.PLAN);
                    refBubble.childrenSelectable = true;
                    refBubble.stripChildren = false;

                    bubbleThing.setPart(Part.REF, refBubble);

                    PGameplayData gpData = new PGameplayData();
                    gpData.eggLink = new EggLink();
                    gpData.eggLink.plan = new ResourceDescriptor(descriptor, ResourceType.PLAN);
                    gpData.eggLink.shareable = shareableCheckBox.isSelected();
                    gpData.keyLink = null;

                    bubbleThing.setPart(Part.GAMEPLAY_DATA, gpData);
                    PGroup grup = new PGroup();
                    grup.planDescriptor = new ResourceDescriptor(31743l, ResourceType.PLAN);
                    bubbleThing.setPart(Part.GROUP, grup);

                    things.add(bubbleThing);
                }

                things.add(groupThing);

                status = new status();
                status.amount.setText(Integer.toString(things.size() - 1));

                Thread thread = new Thread() {
                    public void run() {
                        try {
                            PWorld w = new PWorld();
                            w.things = things;
                            level.world.setPart(Part.WORLD, w);
                            status.close();
                            status = null;

                            byte[] compressed = Resource.compress(level.build(new Revision(Branch.LEERDAMMER.getHead(), Branch.LEERDAMMER.getID(), Revisions.LD_LAMS_KEYS), CompressionFlags.USE_ALL_COMPRESSION));

                            File file = FileChooser.openFile(bubblebombCheckBox.isSelected() ? "Bubblebomb" : "Bubblecolumn", "bin", true, false)[0];
                            try {
                                Files.write(file.toPath(), compressed);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }

                            this.stop();
                        } catch(Exception v) {
                            v.printStackTrace();
                            this.stop();
                        }
                    }
                };

                thread.start();
            }
        });
    }
}
