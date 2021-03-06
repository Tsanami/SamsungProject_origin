package com.example.samsungproject;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.samsungproject.gameobjects.Joystick;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Game extends SurfaceView implements SurfaceHolder.Callback {

    private final Context context;
    private Joystick joystickWalk; // Джойстик для ходьбы
    private Joystick joystickGun; // Джойстик для оружия
    private Player player; // Игрок
    Countable countable;
    GameOver gameOver;
    Shop shop;
    public static Resources res;
    GameLoop gameLoop;
    int hs, ws;//ширина и высота области рисования
    boolean isFirstDraw = true;
    boolean isDmgHpDraw = false;
    boolean playerAlive = true;
    GameMap gameMap;
    private List<Spell> spellList = new ArrayList<Spell>();
    private List<Enemy> enemyList = new ArrayList<>();
    private int numberOfSpellsToCast = 0;
    private float touchX, touchY;
    int coins=1;
    int score=1;

    // Высчитывание кадров
    private int updatesTime = 5;
    private final int  MAX_UPDATES_PER_SECOND = 20;

    private Melee melee;

    private Bitmap weaponShifter;

    public Game(Context context) {
        super(context);
        getHolder().addCallback(this);
        res = getResources();

        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        this.context = context;
        gameLoop = new GameLoop(this, surfaceHolder);

        setFocusable(true);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
        if (gameLoop.getState().equals(Thread.State.TERMINATED)) {
            SurfaceHolder surfaceHolder = getHolder();
            surfaceHolder.addCallback(this);
            gameLoop = new GameLoop(this, surfaceHolder);
        }
        gameLoop.startLoop();
    }


    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        gameLoop.setRunning(false);
        try {
            gameLoop.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if(isFirstDraw){
            hs = getHeight();
            ws = getWidth();
            countable = new Countable();
            gameOver = new GameOver();
            joystickWalk = new Joystick( ws/8, (int)(hs-(hs/5)), 140, 80, context);
            joystickGun = new Joystick(ws - ws/4+50, (int)(hs-(hs/5)), 140, 80, context);
            shop = new Shop(ws, hs);
            player = new Player(context,ws / 2, (hs)-250, joystickWalk);
            isFirstDraw = false;
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            melee = new Melee(context, 1550, 550);
        }

        //gameMap.draw(canvas); // Рисовать карту
        if (gameOver.isPlayerAlive()){
            if (melee.isMelee())
                melee.drawMelee(canvas);
            else
                joystickGun.drawGun(canvas); // Рисовать джойстик для пушки
            melee.drawWeaponShifter(canvas);

            shop.drawMenu(canvas);
            if (shop.getIsPressed() && isDmgHpDraw){
                shop.drawHPUP(canvas);
                shop.drawDMGUP(canvas);
                shop.drawCloseMenu(canvas);
            }
            countable.drawCoins(canvas);
            countable.drawScore(canvas);
            countable.drawHp(canvas, player);
            player.draw(canvas); // Рисовать игрока
            for (Spell spell: spellList) {
                spell.draw(canvas);
            }
            for (Enemy enemy: enemyList){
                enemy.draw(canvas);
            }
            joystickWalk.drawWalk(canvas); // Рисовать джойстик для ходьбы
        }
        if (player.getHP()<=0){
            gameOver.setPlayerAlive(false);
            gameOver.drawGameOver(canvas, ws, hs);
        }
        update();
    }


    public void update(){
        if (gameOver.isPlayerAlive()){
            player.update();
            joystickWalk.update();
            joystickGun.update();

            if (player.isJump) player.jump();

            while (numberOfSpellsToCast > 0){
                spellList.add(new Spell(getContext(), player, touchX, touchY, joystickGun));
                numberOfSpellsToCast--;
            }

            if (Enemy.readyToSpawn()){
                enemyList.add(new Enemy(getContext(), player));
            }
            for (Enemy enemy: enemyList) {
                enemy.update();
            }

            Iterator<Enemy> enemyIterator = enemyList.iterator();
            while (enemyIterator.hasNext()){
                Enemy enemy = enemyIterator.next();
                if (Enemy.isColliding(enemy, player)){
                    player.minHp(enemy.getHp());
                    enemyIterator.remove();
                    continue;
                }
                Iterator<Spell> spellIterator = spellList.iterator();
                while (spellIterator.hasNext()){
                    Spell spell = spellIterator.next();
                    if (Enemy.isSpellColliding(enemy, spell)){
                        enemy.setHp(player.getDmg());
                        if (enemy.getHp() <= 0){
                            countable.setCoins(coins);
                            countable.setScore(score);
                            enemyIterator.remove();
                        }
                        Log.d("aaaa", "dadada");
                        spellIterator.remove();
                        break;
                    }
                }
            }
            for (Spell spell: spellList) {
                spell.update();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount()-1;

        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (player.jumpIsPressed(event.getX(1), event.getY(1)))
                    player.isJump = true; // Player's jump
                if (melee.shifterIsPressed(event.getX(1), event.getY(1))&& (!melee.isMelee()))
                    melee.setMelee(true);
                else if (melee.shifterIsPressed(event.getX(1), event.getY(1)) && (melee.isMelee()))
                    melee.setMelee(false);
                if (joystickWalk.isPressed(event.getX(1), event.getY(1))) {
                    joystickWalk.setIsPressed(true);
                }
                if (joystickGun.isPressed(event.getX(1), event.getY(1))) {
                    joystickGun.setIsPressed(true);
                }
                break;
            case MotionEvent.ACTION_DOWN:
                if (!gameOver.isPlayerAlive()){
                    gameOver.setPlayerAlive(true);
                    player.setHp(20);
                    player.setDmg(1);
                }
                if (shop.menuIsPressed(event.getX(), event.getY())){
                    isDmgHpDraw = true;
                    shop.setIsPressed(true);
                }
                if (isDmgHpDraw){
                    if (shop.closeMenuIsPressed(event.getX(), event.getY())){
                        isDmgHpDraw = false;
                        shop.setIsPressed(false);
                    }
                    if (shop.hpIsPressed(event.getX(), event.getY()) && countable.getCoins()>=10){
                        countable.minCoin(10);
                        player.setHp(5);
                    }
                    if (shop.dmgIsPressed(event.getX(), event.getY()) && countable.getCoins()>=10){
                        countable.minCoin(10);
                        player.setDmg(2);
                    }
                }
                if (player.jumpIsPressed(event.getX(), event.getY())) player.isJump = true;
                if (melee.shifterIsPressed(event.getX(), event.getY()) && (!melee.isMelee())) melee.setMelee(true);
                else if (melee.shifterIsPressed(event.getX(), event.getY()) && (melee.isMelee()))
                    melee.setMelee(false);
                if (joystickWalk.isPressed(event.getX(), event.getY())){
                    joystickWalk.setIsPressed(true);
                    if (joystickWalk.getIsPressed() && !joystickGun.isFirst())
                        joystickWalk.setIsFirst(true);
                }else if (joystickGun.isPressed(event.getX(), event.getY())){
                    joystickGun.setIsPressed(true);
                    if (joystickGun.getIsPressed() && !joystickWalk.isFirst())
                        joystickGun.setIsFirst(true);
                }
                if (shop.menuIsPressed(event.getX(), event.getY())){
                    shop.setIsPressed(true);
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (joystickGun.getIsPressed() && joystickGun.isFirst() && !melee.isMelee()){
                    if (pointerCount==0) {
                        moveGunJk(0, joystickGun.getIsPressed(), event);
                        if (joystickGun.getIsPressed() && isTime()) {
                            updatesTime = 0;
                            touchX = event.getX();
                            touchY = event.getY();
                            numberOfSpellsToCast++;
                        }
                        updatesTime++;
                    }
                    else if (joystickWalk.getIsPressed()){
                        moveGunJk(0, joystickGun.getIsPressed(), event);
                        if (joystickGun.getIsPressed() && isTime()&& !melee.isMelee()) {
                            updatesTime = 0;
                            touchX = event.getX();
                            touchY = event.getY();
                            numberOfSpellsToCast++;
                        }
                        updatesTime++;
                        moveWalkJk(1, joystickWalk.getIsPressed(), event);
                    }
                }
                else if (joystickWalk.getIsPressed() && joystickWalk.isFirst()){
                    if (pointerCount==0)
                        moveWalkJk(0, joystickWalk.getIsPressed(), event);
                    else if (joystickWalk.getIsPressed()){
                        moveWalkJk(0, joystickWalk.getIsPressed(), event);
                        moveGunJk(1, joystickGun.getIsPressed(), event);
                        if (joystickGun.getIsPressed() && isTime()&& !melee.isMelee()) {
                            updatesTime = 0;
                            touchX = event.getX();
                            touchY = event.getY();
                            numberOfSpellsToCast++;
                        }
                        updatesTime++;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (joystickGun.getIsPressed() && joystickGun.isFirst()){
                    if (pointerCount==0) {
                        joystickGun.clear();
                        updatesTime=0;
                        break;
                    }
                }
                if (joystickWalk.getIsPressed() && !joystickWalk.isPressed(touchX, touchY)) {
                    joystickWalk.clear();
                    break;
                }
                break;
        }

        return true;
    }


    public void moveGunJk(int pointerId, boolean isPressed, MotionEvent e){
        if (isPressed)
            joystickGun.setCtrlCoef(e.getX(pointerId), e.getY(pointerId));
    }
    public void moveWalkJk(int pointerId, boolean isPressed, MotionEvent e){
        if (isPressed)
            joystickWalk.setCtrlCoef(e.getX(pointerId), e.getY(pointerId));
    }

    public void weaponShifter(){

    }
    public void pause() {
        gameLoop.stopLoop();
    }
    public boolean isTime(){
        return updatesTime == MAX_UPDATES_PER_SECOND;
    }

    public int getGameWidth(){
        return (int)ws;
    }
    public int getGameHeights(){
        return (int)hs;
    }
}