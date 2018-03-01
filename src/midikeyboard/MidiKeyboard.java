/*
Program: MidiKeyboard
Last modified: 2016/11/29
Programmed by: Christopher Eric Davis
Description:
    This program accesses the midi device of the computer to turn the
        computer's keyboard into a midi keyboard in the key of C.

    The shift key sharps everything while held down,
        and the control (Ctrl) key flats everything while held down.
 */
package midikeyboard;


import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
//import javax.sound.midi.Transmitter; //(Not needed.  No physical device)
import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;



public class MidiKeyboard {
    final static int 
            A_LOW=0, //-1 octave
            B_LOW=1, 
            C_LOW=2, 
            D_LOW=3, 
            E_LOW=4, 
            F_LOW=5, 
            G_LOW=6,
            A_MID=7, //center octave
            B_MID=8, 
            C_MID=9, 
            D_MID=10, 
            E_MID=11,
            F_MID=12,
            G_MID=13,
            A_HIGH=14,//+1 octave
            B_HIGH=15,
            C_HIGH=16,
            D_HIGH=17,
            E_HIGH=18,
            F_HIGH=19,
            G_HIGH=20,
            A_HIGHER=21,//+2 octaves
            B_HIGHER=22,
            C_HIGHER=23,
            D_HIGHER=24,
            E_HIGHER=25,
            F_HIGHER=26,
            G_HIGHER=27;
    final static int[] NOTES={45, 47, 48, 50, 52, 53, 55}; //abcdefg
    static int sharpFlat=0; //+1 for sharp, -1 for flat
    static boolean sharpOn, flatOn;
    static Synthesizer synth;
    static Sequencer mySequencer;
    static Receiver myReceiver;
//    static Transmitter myTransmitter;  //not needed since no physical device
    static Info[] deviceInfo;
    static Instrument[] orchestra;
    static MidiDevice device;
    static MidiChannel[] synthChannel;
    static ShortMessage message;
    static Soundbank defaultSoundbank;
    static boolean[] noteOn, notePlaying;
    static JFrame frame;
    static JPanel panel;
    static Timer t;
    
    public static void main(String[] args) {
        noteOn = new boolean[G_HIGHER+1];
        notePlaying = new boolean[G_HIGHER+1];
        
        //get info about installed midi devices
        deviceInfo = MidiSystem.getMidiDeviceInfo();
        if (deviceInfo.length > 0){
            //output details
            System.out.println(deviceInfo.length + " devices found");
            for (int i = 0; i < deviceInfo.length; ++i){
                System.out.println(deviceInfo[i].getName() + " - " +
                    deviceInfo[i].getDescription()+ "\n");
            }
                    
        }
        
        
        try {
            // get device from list
            device = MidiSystem.getMidiDevice(deviceInfo[0]);
            //open device for use
            if(!device.isOpen()) {
                device.open();
            }
            
            
            mySequencer = MidiSystem.getSequencer(true);
            if (!mySequencer.isOpen()){
                mySequencer.open();
            }
            //note comes in...  (not needed, not dealing with physical device)
//            myTransmitter = mySequencer.getTransmitter();
            
            synth = MidiSystem.getSynthesizer();
            if (!synth.isOpen()) {
                synth.open();
            }
            
            //send to here
            myReceiver = synth.getReceiver();
            //each transmitter only sends to one receiver at a time.
//            myTransmitter.setReceiver(myReceiver);
        } catch (MidiUnavailableException ex) {
            System.err.println(ex.getMessage());
        }
        
        System.out.println("Device details\n" +
                "Max receivers of device: " + device.getMaxReceivers() +
                "\nMax transmitters of device: " + device.getMaxTransmitters() +
                "\nMicrosecond timestamp: " + device.getMicrosecondPosition());
        
        synthChannel = synth.getChannels();
        System.out.println("Synth Details\nChannels: " + synthChannel.length +
                " Latency: " + synth.getLatency() +
                "\nMax notes at once:" + synth.getMaxPolyphony());
        
        defaultSoundbank= synth.getDefaultSoundbank();
        System.out.println("Default Soundbank:"+ defaultSoundbank.getName() +
                "\nVendor: " + defaultSoundbank.getVendor() + "\nVersion: " +
                defaultSoundbank.getVersion() + "\nDescription: " +
                defaultSoundbank.getDescription() + "\n");
        
        //what instruments are available?
        orchestra = defaultSoundbank.getInstruments();
        if (orchestra.length > 0){
            System.out.println(orchestra.length + " instruments found.");
            
            //list them nicely
            for (int i = 1; i <= orchestra.length; ++i){
                System.out.print(orchestra[i-1].getName() +
                        ((i%10==0)?"\n" :"\t"));
            }
        }
        
        
        initUI();
        while (frame.isVisible()){}
        //close all
        myReceiver.close();
        mySequencer.close();
//        myTransmitter.close();
        synth.close();
    }
    
    /**
     * Play the passed midi note value on the passed receiver
     */
    private static void playNote(int note, Receiver inst){
        
        ShortMessage n = new ShortMessage();
        try {
            //command, channel, Note, Volume
            n.setMessage(ShortMessage.NOTE_ON, 0, note, 93);
        } catch (InvalidMidiDataException ex) {
            System.err.println(ex.getMessage());
        }
        inst.send(n, 0);
    }
    
    /**
     * Stop playing the passed midi note value on the passed receiver.
     */
    private static void stopNote(int note, Receiver inst){
        ShortMessage n = new ShortMessage();
        try {
            //command, channel, Note, Volume
            n.setMessage(ShortMessage.NOTE_OFF, 0, note, 93);
        } catch (InvalidMidiDataException ex) {
            System.err.println(ex.getMessage());
        }
        inst.send(n, 0);
    }
    
    private static void initUI(){
        frame = new JFrame("MidiKeyboard");
        panel = new JPanel();
        GridLayout grid =new GridLayout(3, 1, 5, 5);
        panel.setLayout(grid);
        
        //instrument selection
        JComboBox listInstruments = new JComboBox();
        for (int i = 1; i <= orchestra.length; ++i){
            listInstruments.addItem(orchestra[i-1].getName());
        }
        listInstruments.addItemListener(new ItemListener(){

            @Override
            public void itemStateChanged(ItemEvent e) {
                //change selected channel's instrument to selected instrument
                synthChannel[0].programChange(
                        //get the bank the instrument is in
                        orchestra[listInstruments.getSelectedIndex()].
                                getPatch().getBank(),
                        //get the instrument itself
                        orchestra[listInstruments.getSelectedIndex()].
                                getPatch().getProgram());
                //switch to panel focus so playing can begin immediately.
                panel.requestFocus();
//                listInstruments.getSelectedIndex();
            }
        });
        panel.add(listInstruments);
        
        //Put instructions on window
        JLabel info = new JLabel(
                "  -  \u266f - Shift for sharp, Ctrl for flat -  \u266f -");
        panel.add(info);
        
        JLabel b = new JLabel("Type something!");
        panel.add(b);
        
        //adding ability to play...
        panel.setFocusable(true);
        panel.addKeyListener(new KeyListener(){

            @Override
            public void keyTyped(KeyEvent e) {
//                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()){
                    //Home Row
                    case KeyEvent.VK_A:
                        noteOn[A_MID]=true;
                        break;
                    case KeyEvent.VK_S:
                        noteOn[B_MID]=true;
                        break;
                    case KeyEvent.VK_D:
                        noteOn[C_MID]=true;
                        break;
                    case KeyEvent.VK_F:
                        noteOn[D_MID]=true;
                        break;
                    case KeyEvent.VK_G:
                        noteOn[E_MID]=true;
                        break;
                    case KeyEvent.VK_H:
                        noteOn[F_MID]=true;
                        break;
                    case KeyEvent.VK_J:
                        noteOn[G_MID]=true;
                        break;
                    case KeyEvent.VK_K:
                        noteOn[A_HIGH]=true;
                        break;
                    case KeyEvent.VK_L:
                        noteOn[B_HIGH]=true;
                        break;
                    case KeyEvent.VK_SEMICOLON:
                        noteOn[C_HIGH]=true;
                        break;
                    case KeyEvent.VK_QUOTE:
                        noteOn[D_HIGH]= true;
                        break;
                    
                    //Above home row
                    case KeyEvent.VK_Q:
                        noteOn[A_HIGH]=true;
                        break;
                    case KeyEvent.VK_W:
                        noteOn[B_HIGH]=true;
                        break;
                    case KeyEvent.VK_E:
                        noteOn[C_HIGH]=true;
                        break;
                    case KeyEvent.VK_R:
                        noteOn[D_HIGH]=true;
                        break;
                    case KeyEvent.VK_T:
                        noteOn[E_HIGH]=true;
                        break;
                    case KeyEvent.VK_Y:
                        noteOn[F_HIGH]=true;
                        break;
                    case KeyEvent.VK_U:
                        noteOn[G_HIGH]=true;
                        break;
                    case KeyEvent.VK_I:
                        noteOn[A_HIGHER]=true;
                        break;
                    case KeyEvent.VK_O:
                        noteOn[B_HIGHER]=true;
                        break;
                    case KeyEvent.VK_P:
                        noteOn[C_HIGHER]=true;
                        break;
                    case KeyEvent.VK_OPEN_BRACKET:
                        noteOn[D_HIGHER]=true;
                        break;
                    case KeyEvent.VK_CLOSE_BRACKET:
                        noteOn[E_HIGHER]=true;
                        break;
                        
                    //below home row
                    case KeyEvent.VK_Z:
                        noteOn[A_LOW]=true;
                        break;
                    case KeyEvent.VK_X:
                        noteOn[B_LOW]=true;
                        break;
                    case KeyEvent.VK_C:
                        noteOn[C_LOW]=true;
                        break;
                    case KeyEvent.VK_V:
                        noteOn[D_LOW]=true;
                        break;
                    case KeyEvent.VK_B:
                        noteOn[E_LOW]=true;
                        break;
                    case KeyEvent.VK_N:
                        noteOn[F_LOW]=true;
                        break;
                    case KeyEvent.VK_M:
                        noteOn[G_LOW]=true;
                        break;
                    case KeyEvent.VK_COMMA:
                        noteOn[A_MID]=true;
                        break;
                    case KeyEvent.VK_PERIOD:
                        noteOn[B_MID]=true;
                        break;
                    case KeyEvent.VK_SLASH:
                        noteOn[C_MID]=true;
                        break;
                        
                    
                    //Shift and control
                    case KeyEvent.VK_SHIFT:
                        if (!sharpOn){
                            ++sharpFlat;
                            sharpOn=true;
                        }
                        break;
                    case KeyEvent.VK_CONTROL:
                        if (!flatOn){
                            --sharpFlat;
                            flatOn=true;
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()){
                    //home row
                    case KeyEvent.VK_A:
                        noteOn[A_MID]=false;
                        break;
                    case KeyEvent.VK_S:
                        noteOn[B_MID]=false;
                        break;
                    case KeyEvent.VK_D:
                        noteOn[C_MID]=false;
                        break;
                    case KeyEvent.VK_F:
                        noteOn[D_MID]=false;
                        break;
                    case KeyEvent.VK_G:
                        noteOn[E_MID]=false;
                        break;
                    case KeyEvent.VK_H:
                        noteOn[F_MID]=false;
                        break;
                    case KeyEvent.VK_J:
                        noteOn[G_MID]=false;
                        break;
                    case KeyEvent.VK_K:
                        noteOn[A_HIGH]=false;
                        break;
                    case KeyEvent.VK_L:
                        noteOn[B_HIGH]=false;
                        break;
                    case KeyEvent.VK_SEMICOLON:
                        noteOn[C_HIGH]=false;
                        break;
                    case KeyEvent.VK_QUOTE:
                        noteOn[D_HIGH]=false;
                        break;
                    
                    //Above home row
                    case KeyEvent.VK_Q:
                        noteOn[A_HIGH]=false;
                        break;
                    case KeyEvent.VK_W:
                        noteOn[B_HIGH]=false;
                        break;
                    case KeyEvent.VK_E:
                        noteOn[C_HIGH]=false;
                        break;
                    case KeyEvent.VK_R:
                        noteOn[D_HIGH]=false;
                        break;
                    case KeyEvent.VK_T:
                        noteOn[E_HIGH]=false;
                        break;
                    case KeyEvent.VK_Y:
                        noteOn[F_HIGH]=false;
                        break;
                    case KeyEvent.VK_U:
                        noteOn[G_HIGH]=false;
                        break;
                    case KeyEvent.VK_I:
                        noteOn[A_HIGHER]=false;
                        break;
                    case KeyEvent.VK_O:
                        noteOn[B_HIGHER]=false;
                        break;
                    case KeyEvent.VK_P:
                        noteOn[C_HIGHER]=false;
                        break;
                    case KeyEvent.VK_OPEN_BRACKET:
                        noteOn[D_HIGHER]=false;
                        break;
                    case KeyEvent.VK_CLOSE_BRACKET:
                        noteOn[E_HIGHER]=false;
                        break;
                        
                    //below home row
                    case KeyEvent.VK_Z:
                        noteOn[A_LOW]=false;
                        break;
                    case KeyEvent.VK_X:
                        noteOn[B_LOW]=false;
                        break;
                    case KeyEvent.VK_C:
                        noteOn[C_LOW]=false;
                        break;
                    case KeyEvent.VK_V:
                        noteOn[D_LOW]=false;
                        break;
                    case KeyEvent.VK_B:
                        noteOn[E_LOW]=false;
                        break;
                    case KeyEvent.VK_N:
                        noteOn[F_LOW]=false;
                        break;
                    case KeyEvent.VK_M:
                        noteOn[G_LOW]=false;
                        break;
                    case KeyEvent.VK_COMMA:
                        noteOn[A_MID]=false;
                        break;
                    case KeyEvent.VK_PERIOD:
                        noteOn[B_MID]=false;
                        break;
                    case KeyEvent.VK_SLASH:
                        noteOn[C_MID]=false;
                        break;
                    
                    //Shift and control
                    case KeyEvent.VK_SHIFT:
                        if (sharpOn){
                            --sharpFlat;
                            sharpOn=false;
                        }
                        break;
                    case KeyEvent.VK_CONTROL:
                        if (flatOn){
                            ++sharpFlat;
                            flatOn=false;
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        
        //timer to check change of notes.
        t = new Timer(1, (ActionEvent e) -> {
            
//            if (!panel.isFocusOwner()) {
//                panel.requestFocus();
//            }
            
            //iterate across notes, checking if it should play or stop
            for (int i = 0; i < noteOn.length; ++i)
                
                //if it should be playing and isn't already...
                if (noteOn[i] && !notePlaying[i]){
                    
                    //play (note) + (octave calculation) + (sharp or flat value)
                    playNote(NOTES[i%NOTES.length] +
                            (12 * (i/NOTES.length)) +
                            sharpFlat, myReceiver);
                    
                    notePlaying[i]=true;
                }
                
                else if (!noteOn[i]){
                    stopNote(NOTES[i%NOTES.length] +
                            (12 * (i/NOTES.length)) +
                            sharpFlat, myReceiver);
                    notePlaying[i]=false;
                }
        });
        t.start();
        
        frame.add(panel);
        frame.setLocationRelativeTo(null);
        frame.setSize(250, 100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        
    }
    
}
