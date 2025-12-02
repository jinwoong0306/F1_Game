import React, { useState } from 'react';
import { SplashScreen } from './components/SplashScreen';
import { LoginScreen } from './components/LoginScreen';
import { MainMenu } from './components/MainMenu';
import { SinglePlayerSetup } from './components/SinglePlayerSetup';
import { MultiPlayerRoom } from './components/MultiPlayerRoom';
import { Leaderboard } from './components/Leaderboard';
import { Settings } from './components/Settings';

export type Screen = 
  | 'splash' 
  | 'login' 
  | 'mainMenu' 
  | 'singlePlayer' 
  | 'multiPlayer' 
  | 'leaderboard' 
  | 'settings';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('splash');

  const renderScreen = () => {
    switch (currentScreen) {
      case 'splash':
        return <SplashScreen onComplete={() => setCurrentScreen('login')} />;
      case 'login':
        return <LoginScreen onComplete={() => setCurrentScreen('mainMenu')} />;
      case 'mainMenu':
        return <MainMenu onNavigate={setCurrentScreen} />;
      case 'singlePlayer':
        return <SinglePlayerSetup onBack={() => setCurrentScreen('mainMenu')} />;
      case 'multiPlayer':
        return <MultiPlayerRoom onBack={() => setCurrentScreen('mainMenu')} />;
      case 'leaderboard':
        return <Leaderboard onBack={() => setCurrentScreen('mainMenu')} />;
      case 'settings':
        return <Settings onBack={() => setCurrentScreen('mainMenu')} />;
      default:
        return <MainMenu onNavigate={setCurrentScreen} />;
    }
  };

  return (
    <div className="w-full h-screen bg-black overflow-hidden">
      {renderScreen()}
    </div>
  );
}
