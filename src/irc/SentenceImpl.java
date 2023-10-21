/***
 * Sentence class : used for keeping the text exchanged between users
 * during a chat application
 * Contact: 
 *
 * Authors: 
 */

package irc;

public class SentenceImpl implements Sentence {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    String data;

    public SentenceImpl() {
        data = new String("");
    }

    @Override
    public void write(String text) {
        data = text;
    }

    @Override
    public String read() {
        return data;
    }
}
